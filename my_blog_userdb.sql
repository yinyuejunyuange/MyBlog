-- MySQL dump 10.13  Distrib 8.0.36, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: my_blog_userdb
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
-- Table structure for table `role_permissions`
--

DROP TABLE IF EXISTS `role_permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role_permissions` (
  `role_id` bigint NOT NULL COMMENT '角色id',
  `permissions_id` bigint NOT NULL COMMENT '权限id',
  PRIMARY KEY (`role_id`,`permissions_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role_permissions`
--

LOCK TABLES `role_permissions` WRITE;
/*!40000 ALTER TABLE `role_permissions` DISABLE KEYS */;
INSERT INTO `role_permissions` VALUES (1,1);
/*!40000 ALTER TABLE `role_permissions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `search`
--

DROP TABLE IF EXISTS `search`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `search` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `content` varchar(256) NOT NULL COMMENT '搜索的文字',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '修改时间',
  `lately_time` datetime NOT NULL COMMENT '最近搜索时间',
  `is_delete` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除 0：未删除 1：删除',
  `is_user_delete` tinyint NOT NULL DEFAULT '0' COMMENT '用户是否删除 0：未删除 1：删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1898670776857657346 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户搜索表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `search`
--

LOCK TABLES `search` WRITE;
/*!40000 ALTER TABLE `search` DISABLE KEYS */;
INSERT INTO `search` VALUES (1,1,'123','2025-03-09 14:00:12','2025-03-09 13:59:57','2025-03-09 14:00:06',0,0),(2,1,'123','2025-03-09 14:00:12','2025-03-09 13:59:57','2025-03-09 14:00:06',0,0),(3,1,'4','2025-03-09 14:00:12','2025-03-09 13:59:57','2025-03-09 14:00:06',0,0),(4,1,'4','2025-03-09 14:00:12','2025-03-09 13:59:57','2025-03-09 14:00:06',0,0),(5,1,'4','2025-03-09 14:00:12','2025-03-09 13:59:57','2025-03-09 14:00:06',0,0),(1898647725004939265,1893237050568912898,'123','2025-03-09 08:11:07','2025-03-09 08:11:07','2025-03-09 08:11:07',0,0),(1898647803740413953,1893237050568912898,'测试','2025-03-09 08:11:26','2025-03-09 08:11:26','2025-03-09 08:11:26',0,0),(1898648118409682946,1893237050568912898,'java','2025-03-09 08:12:41','2025-03-09 08:12:41','2025-03-09 08:12:41',0,0),(1898653627909005313,1893237050568912898,'git','2025-03-09 08:34:35','2025-03-09 08:34:35','2025-03-09 08:34:35',0,0),(1898670776857657345,1,'java','2025-03-09 09:42:43','2025-03-09 09:42:43','2025-03-09 09:42:43',0,0);
/*!40000 ALTER TABLE `search` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_permissions`
--

DROP TABLE IF EXISTS `sys_permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_permissions` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `permissions_name` varchar(32) NOT NULL COMMENT '权限名称',
  `create_time` datetime NOT NULL COMMENT '创建日期',
  `update_time` datetime NOT NULL COMMENT '修改日期',
  `is_delete` int NOT NULL COMMENT '删除 1：删除 0：未删除',
  `is_stop` int NOT NULL COMMENT '是否被禁用 ：1 禁 2：未禁',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_permissions`
--

LOCK TABLES `sys_permissions` WRITE;
/*!40000 ALTER TABLE `sys_permissions` DISABLE KEYS */;
INSERT INTO `sys_permissions` VALUES (1,'writer','2025-02-22 11:17:34','2025-02-22 11:17:40',0,0);
/*!40000 ALTER TABLE `sys_permissions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `role_name` varchar(32) NOT NULL COMMENT '名称',
  `create_time` datetime NOT NULL COMMENT '创建日期',
  `update_time` datetime NOT NULL COMMENT '修改日期',
  `is_delete` int NOT NULL COMMENT '删除 1：删除 0：未删除',
  `is_stop` int NOT NULL COMMENT '是否被禁用 ：1 禁 2：未禁',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role`
--

LOCK TABLES `sys_role` WRITE;
/*!40000 ALTER TABLE `sys_role` DISABLE KEYS */;
INSERT INTO `sys_role` VALUES (1,'user','2025-02-22 11:18:03','2025-02-22 11:18:11',0,0);
/*!40000 ALTER TABLE `sys_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` varchar(32) NOT NULL COMMENT '名称',
  `password` varchar(64) NOT NULL COMMENT '密码——加密后',
  `image_url` varchar(256) NOT NULL COMMENT '用户头像文件路径',
  `sex` int DEFAULT NULL COMMENT '用户性别',
  `create_time` datetime NOT NULL COMMENT '创建日期',
  `update_time` datetime NOT NULL COMMENT '修改日期',
  `is_delete` int NOT NULL COMMENT '删除 1：删除 0：未删除',
  `is_freeze` int NOT NULL COMMENT '是否被封禁 ：1 封禁 2：未封禁',
  `email` varchar(64) DEFAULT NULL COMMENT '用户邮箱',
  `introduce` varchar(256) DEFAULT NULL COMMENT '用户简介',
  `star` bigint DEFAULT '0' COMMENT '用户粉丝数',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1897503922344714242 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (1,'oyyjs','$2a$10$UnYvhVqb.WqI9Yuh04lJLeqY0OWLP.jNfh9Sm/7QG4Ot.eA8jBmAy','504e0d1d-c龙 少女 精美动漫4K壁纸_彼岸图网.jpg',0,'2022-01-01 12:00:00','2022-01-01 12:00:00',0,0,'3129544205@qq.com','123123',1),(1893237050568912898,'zly','$2a$10$RRRGxgRBLYagN3XbEqqyOO9B5Lu42U6AvssIinT6Jm20D.ZUJJbC.','man.jpg',1,'2025-02-22 09:51:02','2025-02-22 09:51:02',0,0,'3129544205@qq.com',NULL,0),(1897503922344714241,'suanli','$2a$10$4JupMrovMUByT2AQHsj7i.dHxqiI.EE7VS/9aK.MDbfBV.p7040U6','/man.jpg',1,'2025-03-06 04:26:04','2025-03-06 04:26:04',0,0,'3129544205@qq.com',NULL,0);
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_attention`
--

DROP TABLE IF EXISTS `user_attention`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_attention` (
  `user_id` bigint NOT NULL COMMENT '用户id',
  `attention_id` bigint NOT NULL COMMENT '被关注者id',
  PRIMARY KEY (`user_id`,`attention_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户关注表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_attention`
--

LOCK TABLES `user_attention` WRITE;
/*!40000 ALTER TABLE `user_attention` DISABLE KEYS */;
INSERT INTO `user_attention` VALUES (1893237050568912898,1);
/*!40000 ALTER TABLE `user_attention` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_comment`
--

DROP TABLE IF EXISTS `user_comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_comment` (
  `user_id` bigint NOT NULL COMMENT '用户id',
  `comment_id` bigint NOT NULL COMMENT '评论id',
  PRIMARY KEY (`user_id`,`comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户点赞的评论';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_comment`
--

LOCK TABLES `user_comment` WRITE;
/*!40000 ALTER TABLE `user_comment` DISABLE KEYS */;
INSERT INTO `user_comment` VALUES (1,1895671313595727874),(1,1895804430688034817),(1,1895804826525474818),(1,1895805660462161921),(1893237050568912898,1898669335464148994);
/*!40000 ALTER TABLE `user_comment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_kudos`
--

DROP TABLE IF EXISTS `user_kudos`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_kudos` (
  `user_id` bigint NOT NULL COMMENT '用户id',
  `blog_id` bigint NOT NULL COMMENT '博客id',
  PRIMARY KEY (`user_id`,`blog_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户点赞表 双主键';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_kudos`
--

LOCK TABLES `user_kudos` WRITE;
/*!40000 ALTER TABLE `user_kudos` DISABLE KEYS */;
INSERT INTO `user_kudos` VALUES (1,1893581843467694082),(1,1896899273258549249),(1893237050568912898,1893581843467694082);
/*!40000 ALTER TABLE `user_kudos` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_reply`
--

DROP TABLE IF EXISTS `user_reply`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_reply` (
  `user_id` bigint NOT NULL COMMENT '用户id',
  `reply_id` bigint NOT NULL COMMENT '回复id',
  PRIMARY KEY (`user_id`,`reply_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户点赞的回复';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_reply`
--

LOCK TABLES `user_reply` WRITE;
/*!40000 ALTER TABLE `user_reply` DISABLE KEYS */;
INSERT INTO `user_reply` VALUES (1,1895762321041813505);
/*!40000 ALTER TABLE `user_reply` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_role`
--

DROP TABLE IF EXISTS `user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_role` (
  `user_id` bigint NOT NULL COMMENT '用户id',
  `role_id` bigint NOT NULL COMMENT '角色id',
  PRIMARY KEY (`user_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_role`
--

LOCK TABLES `user_role` WRITE;
/*!40000 ALTER TABLE `user_role` DISABLE KEYS */;
INSERT INTO `user_role` VALUES (1,1),(1893235661268680706,1),(1893237050568912898,1),(1897503922344714241,1);
/*!40000 ALTER TABLE `user_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_star`
--

DROP TABLE IF EXISTS `user_star`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_star` (
  `user_id` bigint NOT NULL COMMENT '用户id',
  `blog_id` bigint NOT NULL COMMENT '博客id',
  PRIMARY KEY (`user_id`,`blog_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户收藏表 双主键';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_star`
--

LOCK TABLES `user_star` WRITE;
/*!40000 ALTER TABLE `user_star` DISABLE KEYS */;
INSERT INTO `user_star` VALUES (1,1893581843467694082),(1,1893845854478217218),(1,1893848556310441986),(1,1894011652698021889),(1,1896899273258549249),(1893237050568912898,1893581843467694082);
/*!40000 ALTER TABLE `user_star` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-03-09 17:53:00
