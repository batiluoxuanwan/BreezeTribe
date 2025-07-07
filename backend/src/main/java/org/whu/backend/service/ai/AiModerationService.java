package org.whu.backend.service.ai;

import com.aliyun.green20220302.Client;
import com.aliyun.green20220302.models.TextModerationRequest;
import com.aliyun.green20220302.models.TextModerationResponse;
import com.aliyun.green20220302.models.TextModerationResponseBody;
import com.aliyun.teaopenapi.models.Config;
import com.aliyun.teautil.models.RuntimeOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.whu.backend.dto.report.ModerationDetails;

/**
 * AI内容审核服务，对文本进行审核。
 */
@Service
@Slf4j
public class AiModerationService {
    // --- 从 application.properties 中注入配置 ---
    @Value("${aliyun.moderation.region-id}")
    private String regionId;

    @Value("${aliyun.moderation.endpoint}")
    private String endpoint;

    @Value("${aliyun.moderation.service-code}") // 【新增】你需要配置的服务代码
    private String serviceCode;

    @Value("${OSS_ACCESS_KEY_ID}")
    private String accessKeyId;

    @Value("${OSS_ACCESS_KEY_SECRET}")
    private String accessKeySecret;

    private Client client; // 【已更换】使用新的SDK客户端

    /**
     * 在服务创建后，初始化一次SDK客户端。
     */
    @PostConstruct
    public void init() {
        try {
            Config config = new Config();
            config.setAccessKeyId(accessKeyId);
            config.setAccessKeySecret(accessKeySecret);
            config.setRegionId(regionId);
            config.setEndpoint(endpoint);
            config.setConnectTimeout(3000);
            config.setReadTimeout(6000);

            this.client = new Client(config);
            log.info("阿里云内容安全【增强版】SDK客户端初始化成功！");
        } catch (Exception e) {
            log.error("阿里云内容安全【增强版】SDK客户端初始化失败！请检查配置。", e);
        }
    }

    /**
     * 【核心方法】审核一段文本内容
     * @param text 要审核的文本
     * @return ModerationResult枚举，代表审核结果
     */
    public ModerationDetails moderateText(String text) throws JSONException {
        if (this.client == null) {
            log.error("AI审核失败：SDK客户端未初始化。");
            return new ModerationDetails(ModerationResult.REVIEW,"内容合规检查SDK未初始化") ; // 保守处理，标记为人工审核
        }
        if (!StringUtils.hasText(text)) {
            return new ModerationDetails(ModerationResult.PASS,"文本为空"); // 空文本直接通过
        }

        // 1. 创建运行时选项
        RuntimeOptions runtime = new RuntimeOptions();
        runtime.connectTimeout = 10000;
        runtime.readTimeout = 10000;

        // 2. 构建检测参数
        JSONObject serviceParameters = new JSONObject();
        serviceParameters.put("content", text);

        TextModerationRequest request = new TextModerationRequest();
        request.setService(serviceCode); // 设置你在阿里云控制台配置的服务代码
        request.setServiceParameters(serviceParameters.toString());

        try {
            // 3. 调用方法获取检测结果
            // log.info("AI审核服务：正在发送文本至增强版API...");
            TextModerationResponse response = client.textModerationWithOptions(request, runtime);

            // 4. 解析响应结果
            if (response != null && response.getStatusCode() == 200) {
                TextModerationResponseBody body = response.getBody();
                if (body != null && body.getCode() == 200) {
                    TextModerationResponseBody.TextModerationResponseBodyData data = body.getData();
                    // log.info("AI审核服务：收到API响应，Labels: [{}], Reason: [{}]", data.getLabels(), data.getReason());

                    // 【核心判断逻辑】根据返回的标签(Labels)来决定结果
                    // 如果Labels不为空，说明命中了某些规则，需要拦截或复审。
                    if (StringUtils.hasText(data.getLabels())) {
                        // 你可以根据具体label做更细致的判断，例如包含"review"的为REVIEW
                        return new ModerationDetails(ModerationResult.BLOCK, data.getReason()) ;
                    } else {
                        // 如果Labels为空，说明内容合规
                        return new  ModerationDetails(ModerationResult.PASS, data.getReason());
                    }
                } else {
                    log.error("AI审核API返回业务错误, Code: {}, Msg: {}", body != null ? body.getCode() : "null", body != null ? body.getMessage() : "null");
                }
            } else {
                log.error("AI审核API返回HTTP错误, Status: {}", response != null ? response.getStatusCode() : "null");
            }
        } catch (Exception e) {
            log.error("调用阿里云内容安全SDK时发生异常！", e);
        }

        // 任何异常或未知情况，都保守地标记为需要人工审核
        return new ModerationDetails(ModerationResult.REVIEW,"自动审核AI异常") ;
    }

    /**
     * AI内容审核的结果枚举
     */
    public enum ModerationResult {
        PASS, REVIEW, BLOCK
    }
}