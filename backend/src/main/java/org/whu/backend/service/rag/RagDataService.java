package org.whu.backend.service.rag;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.whu.backend.entity.Tag;
import org.whu.backend.entity.travelpac.Route;
import org.whu.backend.entity.Spot;
import org.whu.backend.entity.travelpac.TravelDeparture;
import org.whu.backend.entity.travelpac.TravelPackage;
import org.whu.backend.entity.association.PackageRoute;
import org.whu.backend.entity.association.RouteSpot;
import org.whu.backend.entity.rag.RagKnowledgeEntry;
import org.whu.backend.repository.rag.RagKnowledgeEntryRepository;
import org.whu.backend.repository.travelRepo.TravelPackageRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RagDataService {

    @Autowired
    private TravelPackageRepository travelPackageRepository;
    @Autowired
    private RagKnowledgeEntryRepository ragKnowledgeEntryRepository;

    /**
     * [核心] 同步所有旅行团数据到RAG知识库表
     * 这个方法可以由一个定时任务调用，或者由管理员手动触发。
     * 它会自动处理新增和更新，实现查重然后追加。
     */
    @Transactional
    public void syncKnowledgeBase() {
        log.info("开始同步RAG知识库...");

        List<TravelPackage> packages = travelPackageRepository.findByStatus(TravelPackage.PackageStatus.PUBLISHED);

        log.info("发现 {} 个已发布的旅行团需要同步。", packages.size());

        for (TravelPackage pkg : packages) {

            // 1. 计算该产品所有团期中的最低价作为“起价”
            Optional<BigDecimal> minPriceOpt = pkg.getDepartures().stream()
                    .filter(d -> d.getStatus() == TravelDeparture.DepartureStatus.OPEN) // 只考虑可报名的团期
                    .map(TravelDeparture::getPrice)
                    .min(Comparator.naturalOrder());

            // 如果没有可报名的团期，可以跳过或设置一个默认行为
            if (minPriceOpt.isEmpty()) {
                log.warn("产品ID '{}' 没有找到任何可报名的团期，跳过同步。", pkg.getId());
                continue;
            }

            String content = buildContentForPackage(pkg,minPriceOpt.get());
            RagKnowledgeEntry entry = new RagKnowledgeEntry();
            entry.setId(pkg.getId());
            entry.setContent(content);
            entry.setName(pkg.getTitle());
            entry.setStartingPrice(minPriceOpt.get());
            entry.setDurationInDays(pkg.getDurationInDays());
            ragKnowledgeEntryRepository.save(entry);
        }

        log.info("RAG知识库同步完成！");
    }

    /**
     * [核心] 导出知识库为CSV格式的字符串
     */
    public String exportKnowledgeBaseToCsv() {
        log.info("开始导出RAG知识库为CSV...");
        List<RagKnowledgeEntry> entries = ragKnowledgeEntryRepository.findAll();

        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("id,content\n");

        for (RagKnowledgeEntry entry : entries) {
            csvBuilder.append(escapeCsvField(entry.getId()));
            csvBuilder.append(",");
            csvBuilder.append(escapeCsvField(entry.getContent()));
            csvBuilder.append("\n");
        }

        log.info("成功导出 {} 条知识库条目。", entries.size());
        return csvBuilder.toString();
    }

    /**
     * [核心] 将所有已发布的旅行团数据导出为XLSX格式的字节数组
     * @return 包含XLSX文件内容的字节数组
     */
    public byte[] exportKnowledgeToXlsxBytes() throws IOException {
        // 1. 【重大改变】直接从 rag_knowledge_base 表中查询所有已处理好的数据
        List<RagKnowledgeEntry> entries = ragKnowledgeEntryRepository.findAll();
        log.info("服务层：准备从知识库导出 {} 条数据到XLSX...", entries.size());

        // 2. 使用 Apache POI 创建一个 XLSX 工作簿
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("旅游团知识库");

            // 3. 创建表头，严格与 RagKnowledgeEntry 实体字段对应
            String[] headers = {"id", "name", "startingPrice", "durationInDays", "content"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            // 4. 【重大改变】填充数据行，直接使用 RagKnowledgeEntry 的字段
            int rowNum = 1;
            for (RagKnowledgeEntry entry : entries) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(entry.getId());
                row.createCell(1).setCellValue(entry.getName());
                row.createCell(2).setCellValue(entry.getStartingPrice().doubleValue());
                row.createCell(3).setCellValue(entry.getDurationInDays());
                row.createCell(4).setCellValue(entry.getContent());
            }

            // 5. 将工作簿写入内存中的字节流
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    /**
     * 将一个TravelPackage实体及其关联的所有信息，转换成一段适合RAG知识库的详细文本。
     *
     * @param travelPackage 从数据库中查询出的，包含完整关联信息的TravelPackage实体。
     * @return 一段为AI大模型精心准备的、结构化的长文本知识。
     */
    private String buildContentForPackage(TravelPackage travelPackage,BigDecimal startingPrice) {
        // 防御性编程，避免传入null导致空指针异常
        if (travelPackage == null) {
            return "";
        }
        // 使用StringBuilder来高效拼接字符串
        StringBuilder contentBuilder = new StringBuilder();
        // 定义一个日期格式化器，让日期更具可读性
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
        // --- Part 1: 拼接产品的核心基础信息 ---
        // 使用清晰的标签，帮助大模型更好地理解各部分内容
        contentBuilder.append("产品标题: ").append(travelPackage.getTitle()).append("\n");
        if (travelPackage.getDealer() != null && travelPackage.getDealer().getUsername() != null) {
            contentBuilder.append("旅游服务提供商: ").append(travelPackage.getDealer().getUsername()).append("\n");
        }

        // 使用“起价”，并移除具体的出发日期
        contentBuilder.append("价格从: ").append(startingPrice.toPlainString()).append("元/人起\n");
        contentBuilder.append("行程总天数: ").append(travelPackage.getDurationInDays()).append("天\n");
        contentBuilder.append("\n");

        // --- Part 2: 【新增】产品特色标签 ---
        if (travelPackage.getTags() != null && !travelPackage.getTags().isEmpty()) {
            String tagString = travelPackage.getTags().stream()
                    .map(Tag::getName)
                    .collect(Collectors.joining(", "));
            contentBuilder.append("## 产品特色标签 ##\n")
                    .append("这个产品包含以下特色：").append(tagString).append("。\n\n");
        }
        contentBuilder.append("\n"); // 空一行，让结构更清晰
        // --- Part 2: 拼接产品的详细文字描述 ---
        contentBuilder.append("## 产品详细介绍 ##\n");
        if (travelPackage.getDetailedDescription() != null && !travelPackage.getDetailedDescription().isEmpty()) {
            contentBuilder.append(travelPackage.getDetailedDescription()).append("\n");
        } else {
            contentBuilder.append("该产品暂无详细的文字介绍。\n");
        }
        contentBuilder.append("\n");
        // --- Part 3: 拼接最核心的每日行程安排 ---
        contentBuilder.append("## 每日行程安排 ##\n");
        // 虽然实体中已经用了@OrderBy，但在代码中再次排序是一种更稳妥的防御性编程习惯
        List<PackageRoute> sortedPackageRoutes = travelPackage.getRoutes().stream()
                .sorted(Comparator.comparingInt(PackageRoute::getDayNumber))
                .toList();
        if (sortedPackageRoutes.isEmpty()) {
            contentBuilder.append("该产品暂未提供详细的每日行程。\n");
        } else {
            // 遍历每一天的行程
            for (PackageRoute packageRoute : sortedPackageRoutes) {
                Route route = packageRoute.getRoute();
                if (route == null) continue; // 如果关联的路线为空，则跳过
                // 使用类似Markdown的标题，让层次更分明
                contentBuilder.append("### 第 ").append(packageRoute.getDayNumber()).append(" 天: ").append(route.getName()).append(" ###\n");
                if (route.getDescription() != null && !route.getDescription().isEmpty()) {
                    contentBuilder.append("本日简介: ").append(route.getDescription()).append("\n");
                }
                // 同样，对一天内的景点也进行排序
                List<RouteSpot> sortedRouteSpots = route.getSpots().stream()
                        .sorted(Comparator.comparingInt(RouteSpot::getOrderColumn))
                        .toList();
                if (sortedRouteSpots.isEmpty()) {
                    contentBuilder.append("- 本日无具体景点安排。\n");
                } else {
                    contentBuilder.append("本日游览景点:\n");
                    // 遍历当天的每一个景点
                    for (RouteSpot routeSpot : sortedRouteSpots) {
                        Spot spot = routeSpot.getSpot();
                        if (spot == null) continue; // 如果关联的景点为空，则跳过
                        contentBuilder.append("- ").append(spot.getName());
                        if (spot.getAddress() != null && !spot.getAddress().isEmpty()) {
                            contentBuilder.append(" (地址: ").append(spot.getCity()).append(spot.getAddress()).append(")");
                        }
                        contentBuilder.append("\n");
                    }
                }
                contentBuilder.append("\n"); // 每天的行程结束后空一行
            }
        }
        // 返回最终拼接好的完整知识文本
        return contentBuilder.toString();
    }

    /**
     * 私有辅助方法：处理CSV中的特殊字符，防止格式错乱
     */
    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            field = field.replace("\"", "\"\"");
            return "\"" + field + "\"";
        }
        return field;
    }
}