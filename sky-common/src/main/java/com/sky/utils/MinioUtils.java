package com.sky.utils;

import cn.hutool.core.lang.UUID;
import com.sky.context.BaseContext;
import io.minio.*;
import io.minio.admin.MinioAdminClient;
import io.minio.admin.messages.info.ErasureSetInfo;
import io.minio.admin.messages.info.Message;
import io.minio.errors.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class MinioUtils {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioAdminClient minioAdminClient;

    @Autowired
    private MinioOSSOperator minioOSSOperator;

    /**
     * 上传文件到MinIO
     * @param file 要上传的文件（Spring MultipartFile）
     * @return 文件在MinIO中的唯一标识（对象名称）
     */
    public String uploadFile(MultipartFile file) throws Exception {
        // 1. 检查存储桶是否存在，不存在则创建
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioOSSOperator.getBucket()).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioOSSOperator.getBucket()).build());
        }

        // 2. 生成文件名（避免重名）
        String originalFilename = file.getOriginalFilename();
        String extendName = "";
        if (originalFilename != null) {
             extendName = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        String objectName = BaseContext.getCurrentId() + "/" + UUID.randomUUID() + extendName;

        // 3. 上传文件
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(minioOSSOperator.getBucket())          // 存储桶名称
                        .object(objectName)          // 对象名称（文件名,可以包含目录, test/随机字符串.txt）
                        .stream(file.getInputStream(), file.getSize(), Long.valueOf("-1"))  // 文件流, 文件大小, 每次读取上传大小，-1表示自动
                        .contentType(file.getContentType())  // 文件类型，比如 image/jpeg
                        .build()
        );

        return objectName;
        //返回结果：test/随机字符串.txt
    }

    /**
     * 上传文件到MinIO
     * @param file 要上传的文件（Spring MultipartFile）
     * @param bucketName 上传到的bucket名称
     * @return 文件在MinIO中的唯一标识（对象名称）
     */
    public String uploadAvatar(MultipartFile file, String bucketName) throws Exception {
        // 1. 检查存储桶是否存在，不存在则创建
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }

        // 2. 生成文件名（避免重名）
        String originalFilename = file.getOriginalFilename();
        String extendName = "";
        if (originalFilename != null) {
            extendName = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        String dir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String objectName = BaseContext.getCurrentId() + "/" + dir + "/" + UUID.randomUUID() + extendName;

        // 3. 上传文件
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)          // 存储桶名称
                        .object(objectName)          // 对象名称（文件名,可以包含目录, test/随机字符串.txt）
                        .stream(file.getInputStream(), file.getSize(), Long.valueOf("-1"))  // 文件流, 文件大小, 每次读取上传大小，-1表示自动
                        .contentType(file.getContentType())  // 文件类型，比如 image/jpeg
                        .build()
        );

        return objectName;
        //返回结果：test/随机字符串.txt
    }

    /**
     * 获取文件临时访问URL（适合前端直接下载）
     * @param objectName 文件在MinIO中的唯一标识
     * @param expiry 有效期（单位：分钟）
     * @return 可访问的URL
     */
    public String getFileUrl(String objectName, int expiry) throws Exception {
        //objectName = "随机字符串.txt"
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(minioOSSOperator.getBucket())
                        .object(objectName)
                        .expiry(expiry, TimeUnit.MINUTES)
                        .build()
        );
        //返回结果：http://localhost:9000/bucket/yyyy/MM/随机字符串.txt?......
    }

    /**
     * 若bucket设置成public属性，则可使用字符串拼接的方式直接访问对应资源
     * @param objectName 调用uploadFile()后返回的文件名，包括路径及扩展名
     * @return 访问url
     */
    public String getFileUrl(String objectName) {
        return minioOSSOperator.getEndpoint() + '/'
                + minioOSSOperator.getBucket() + '/' + objectName;
    }

    /**
     * 若bucket设置成public属性，则可使用字符串拼接的方式直接访问对应资源
     * @param objectName 调用uploadFile()后返回的文件名，包括路径及扩展名
     * @param bucketName 指定要存储到的bucket
     * @return 访问url
     */
    public String getAvatarUrl(String objectName, String bucketName) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
            throw new RuntimeException("存储桶" + bucketName + "不存在");
        }
        return minioOSSOperator.getEndpoint() + '/'
                + bucketName + '/' + objectName;
    }

    /**
     * 获取到宿主机的IP地址，通常从环境变量和配置文件中获取.
     * @return http://宿主机ip:端口号
     */
    public String getHostAddress() {
        return "http://" + minioOSSOperator.getExternalHost();
    }

    /**
     * 删除文件
     * @param objectName 文件在MinIO中的唯一标识
     */
    public void deleteFile(String objectName) throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        //objectName = "随机字符串.txt"
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(minioOSSOperator.getBucket())
                        .object(objectName)
                        .build()
        );
    }

    /**
     * 获取Bucket里面所有的文件，并返回一个迭代器对象
     * @return 一个迭代器对象
     */
    public Iterable<Result<Item>> listAllObjects() {
        return minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(minioOSSOperator.getBucket())
                        .recursive(true)
                        .build()
        );
    }

    /**
     * 根据前缀，递归列出桶下该前缀所有对象（用于删除文件夹）
     * @param prefix 对象前缀，例："1001/docs/" 末尾带斜杠
     * @return 迭代器
     */
    public Iterable<Result<Item>> listAllObjects(String prefix) {
        return minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(minioOSSOperator.getBucket())
                        .prefix(prefix)
                        .recursive(true) // true：递归子目录全部列出
                        .build()
        );
    }

    /**
     * 删除文件夹（前缀下全部对象）
     * @param prefix 文件夹objectName，末尾必须带 /
     */
    public void deleteFolder(String prefix) throws Exception {
        Iterable<Result<Item>> results = listAllObjects(prefix);
        for (Result<Item> result : results) {
            Item item = result.get();
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioOSSOperator.getBucket())
                            .object(item.objectName())
                            .build()
            );
        }
    }

    /**
     * 下载：从MinIO服务器下载指定存储桶（bucket）中的文件
     *
     * @param objectName 指定要从MinIO下载的文件对象名称（即文件路径和文件名）。
     * @return 流 对象
     * @throws Exception 异常
     */
    public InputStream downloadFile(String objectName) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(minioOSSOperator.getBucket())
                        .object(objectName)
                        .build());
    }

    /**
     * 获取所有集群中所有磁盘的总空间大小
     * @return
     */
    public Long getTotalStorage() throws Exception {
        Message serverInfo = minioAdminClient.getServerInfo();
        BigDecimal bigDecimalTotalSpace = new BigDecimal(BigInteger.ZERO);

        Map<Integer, Map<Integer, ErasureSetInfo>> pools = serverInfo.pools();

        for (Map<Integer, ErasureSetInfo> pool : pools.values()) {
            for (ErasureSetInfo setInfo : pool.values()) {
                bigDecimalTotalSpace = bigDecimalTotalSpace.add(setInfo.rawCapacity());
            }
        }

        return bigDecimalTotalSpace.longValue();
    }

    /**
     * 获取所有集群的所有磁盘的所有占用空间
     * @return
     * @throws Exception
     */
    public Long getUsedStorage() throws Exception {
        Message serverInfo = minioAdminClient.getServerInfo();
        BigDecimal bigDecimalTotalUsage = new BigDecimal(BigInteger.ZERO);

        Map<Integer, Map<Integer, ErasureSetInfo>> pools = serverInfo.pools();

        for (Map<Integer, ErasureSetInfo> pool : pools.values()) {
            for (ErasureSetInfo setInfo : pool.values()) {
                bigDecimalTotalUsage = bigDecimalTotalUsage.add(setInfo.rawUsage());
            }
        }

        return bigDecimalTotalUsage.longValue();
    }

    public String getBucketName() {
        return minioOSSOperator.getBucket();
    }
}
