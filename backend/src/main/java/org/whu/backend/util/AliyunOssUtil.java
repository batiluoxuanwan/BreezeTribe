package org.whu.backend.util;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.EnvironmentVariableCredentialsProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyuncs.exceptions.ClientException;

import java.io.InputStream;
import java.net.URL;
import java.util.Date;

public class AliyunOssUtil {

    private static final String ENDPOINT = "https://oss-cn-hangzhou.aliyuncs.com";
    private static final String REGION = "cn-hangzhou";
    private static final String BUCKET_NAME = "breeze-tribe";

    private static final OSS ossClient;

    // 静态代码块初始化 OSSClient
    static {
        // 从环境变量中获取访问凭证
        EnvironmentVariableCredentialsProvider credentialsProvider;
        try {
            credentialsProvider = CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();
        } catch (ClientException e) {
            throw new RuntimeException(e);
        }

        ClientBuilderConfiguration conf = new ClientBuilderConfiguration();
        conf.setSignatureVersion(SignVersion.V4);

        ossClient = OSSClientBuilder.create()
                .endpoint(ENDPOINT)
                .credentialsProvider(credentialsProvider)
                .region(REGION)
                .build();
    }

    /**
     * 上传文件到指定路径
     * @param objectName  OSS 包含路径的对象名，例如 user/10001/avatar.png
     * @param inputStream 文件流
     */
    public static void upload(String objectName, InputStream inputStream) {
        ossClient.putObject(BUCKET_NAME, objectName, inputStream);
        //return "https://" + BUCKET_NAME + ".oss-" + REGION + ".aliyuncs.com/" + objectName;
    }

    /**
     * 删除指定路径的文件
     * @param objectName OSS 包含路径的对象名，例如 user/10001/avatar.png
     */
    public static void delete(String objectName) {
        ossClient.deleteObject(BUCKET_NAME, objectName);
    }

    /**
     * 生成阿里云OSS对象的预签名URL (用于GET请求下载/查看)。
     * @param objectKey              存储在OSS上的对象的完整路径 (例如: "travel-posts/images/user123/image.jpg")
     * @param validityDurationMillis URL的有效时长，单位为毫秒 (例如：TimeUnit.HOURS.toMillis(1) 表示1小时)
     * @return 生成的预签名URL字符串，如果生成失败则返回null（或在内部处理异常）
     */
    public static String generatePresignedGetUrl(String objectKey, long validityDurationMillis) {
        // 计算绝对的过期时间点
        Date expirationTime = new Date(System.currentTimeMillis() + validityDurationMillis);
        // 生成一个GET请求的预签名URL。
        URL presignedUrl = ossClient.generatePresignedUrl(BUCKET_NAME, objectKey, expirationTime);
        return presignedUrl.toString();
    }


    /**
     * 生成阿里云OSS对象的预签名URL，并附带图片处理参数 (用于GET请求获取处理后的图片)。
     * @param objectKey              存储在OSS上的对象的完整路径
     * @param validityDurationMillis URL的有效时长，单位为毫秒
     * @param imageProcessStyleName  阿里云OSS图片处理样式名称 (例如："style/watermark")
     *                               或者 图片处理参数字符串 (例如："image/resize,l_1600/quality,q_75")
     *                               如果为null或空，则不应用任何图片处理。
     * @return 生成的带图片处理参数的预签名URL字符串，如果生成失败则返回null
     */
    public static String generatePresignedGetUrl(String objectKey, long validityDurationMillis, String imageProcessStyleName) {
        Date expirationTime = new Date(System.currentTimeMillis() + validityDurationMillis);
        // 创建一个GeneratePresignedUrlRequest对象，以便我们可以设置图片处理参数
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(BUCKET_NAME, objectKey, HttpMethod.GET);
        request.setExpiration(expirationTime);
        request.addQueryParameter("x-oss-process", imageProcessStyleName);
        URL presignedUrl = ossClient.generatePresignedUrl(request);
        return presignedUrl.toString();
    }

    // 从url获取路径
    public static String extractObjectNameFromUrl(String url) {
        // 例子: https://your-bucket.oss-region.aliyuncs.com/user/10001/avatar.png
        // 提取 user/10001/avatar.png
        int index = url.indexOf(".com/");
        if (index != -1 && index + 5 < url.length()) {
            return url.substring(index + 5);
        }
        return null;
    }

    /**
     * 关闭 OSS 客户端（可选，项目退出时关闭）
     */
    public static void shutdown() {
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }
}