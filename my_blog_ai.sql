-- MySQL dump 10.13  Distrib 8.0.36, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: my_blog_ai
-- ------------------------------------------------------
-- Server version	8.0.36

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `file`
--

DROP TABLE IF EXISTS `file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `file` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `file_address` varchar(256) NOT NULL COMMENT '文件地址',
  `file_name_json` varchar(256) DEFAULT NULL COMMENT '文件上传到工作区的名称',
  `is_upload` int NOT NULL DEFAULT '0' COMMENT '是否上传成功（1成功，0 不成功）',
  `is_delete` int NOT NULL DEFAULT '0' COMMENT '是否删除 1删除 0未删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1897232938459054082 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `file`
--

LOCK TABLES `file` WRITE;
/*!40000 ALTER TABLE `file` DISABLE KEYS */;
INSERT INTO `file` VALUES (1897225375910723586,'H:/MyBlogFiles/ai文档上传测试04.txt',NULL,1,0),(1897226670201749505,'H:/MyBlogFiles/ai文档上传测试05.txt','custom-documents/upload-1628805919716828845-f5258d69-56a4-414c-bbb5-db2d332ee916.json',1,0),(1897228415980126210,'H:/MyBlogFiles/ai文档上传测试06.txt','custom-documents/upload-4775157910186176498-09a32a7c-6dd2-476c-8dbb-2ed04c5da2b6.json',1,0),(1897230262241443841,'H:/MyBlogFiles/Java_反射基础原理剖析.txt','custom-documents/upload-12356067774192627699-1b661947-56c8-442d-91de-107a72e4c2e5.json',1,0),(1897232626272813057,'H:/MyBlogFiles/Java_反射的丰富应用场景.txt','custom-documents/upload-5142566116744223673-f799ed19-70e9-46ab-aec0-dbf835772f15.json',1,0),(1897232938459054081,'H:/MyBlogFiles/Java_反射性能优化策略.txt','custom-documents/upload-16387459480886791377-2902ff98-d627-4a1f-bcd2-b3dae1c4572a.json',1,0);
/*!40000 ALTER TABLE `file` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-03-09 17:53:17
