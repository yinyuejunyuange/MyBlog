-- MySQL dump 10.13  Distrib 8.0.36, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: my_blog_admindb
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
-- Table structure for table `admin`
--

DROP TABLE IF EXISTS `admin`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` varchar(32) NOT NULL COMMENT '管理员名称 真实姓名',
  `password` varchar(64) NOT NULL COMMENT 'password 加密后的密码',
  `image_url` varchar(256) NOT NULL COMMENT '管理员头像',
  `phone` varchar(11) NOT NULL COMMENT '用户电话号码',
  `email` varchar(64) NOT NULL COMMENT '用户邮箱',
  `create_time` datetime NOT NULL COMMENT '用户创建时间',
  `create_by` bigint NOT NULL COMMENT '用户创建者：代表者创建这个的管理员账号的 系统管理员id 如果为0 则代表此管理员是系统管理员',
  `update_time` datetime NOT NULL COMMENT '用户修改时间',
  `update_by` bigint NOT NULL COMMENT '同create_by 但是这个也可以是普通管理员自己修改',
  `is_delete` int NOT NULL COMMENT '逻辑删除标志 1：删除 0：未删除',
  `is_freeze` int NOT NULL COMMENT '是否冻结 1:冻结 2：未冻结',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理员信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `admin`
--

LOCK TABLES `admin` WRITE;
/*!40000 ALTER TABLE `admin` DISABLE KEYS */;
INSERT INTO `admin` VALUES (1,'周琳懿','$2a$10$RRRGxgRBLYagN3XbEqqyOO9B5Lu42U6AvssIinT6Jm20D.ZUJJbC.','girl.jpg','13688160328','1955419130@qq.com','2025-03-12 11:03:58',0,'2025-03-12 11:04:04',0,0,0),(2,'欧阳悦佳','$2a$10$UnYvhVqb.WqI9Yuh04lJLeqY0OWLP.jNfh9Sm/7QG4Ot.eA8jBmAy','boy.jpg','19382111930','3129544205@qq.com','2025-03-12 11:36:46',0,'2025-03-12 11:36:51',0,0,0);
/*!40000 ALTER TABLE `admin` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `admin_role`
--

DROP TABLE IF EXISTS `admin_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin_role` (
  `admin_id` bigint NOT NULL COMMENT '管理员id',
  `role_id` bigint NOT NULL COMMENT '角色id',
  `is_valid` int NOT NULL DEFAULT '1' COMMENT '标识 此关联是否 有效（1：有效 0 失效）',
  PRIMARY KEY (`admin_id`,`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理员角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `admin_role`
--

LOCK TABLES `admin_role` WRITE;
/*!40000 ALTER TABLE `admin_role` DISABLE KEYS */;
INSERT INTO `admin_role` VALUES (2,2,1);
/*!40000 ALTER TABLE `admin_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `permission`
--

DROP TABLE IF EXISTS `permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `permission` varchar(32) NOT NULL COMMENT '管理员权限',
  `is_using` int NOT NULL DEFAULT '1' COMMENT '标识当前权限 是否还在使用(1：使用中 0：暂停使用)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理员权限表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `permission`
--

LOCK TABLES `permission` WRITE;
/*!40000 ALTER TABLE `permission` DISABLE KEYS */;
INSERT INTO `permission` VALUES (1,'manage_users',1),(2,'manage_blogs',1),(3,'manage_comments',1),(4,'manage_report',1),(5,'manage_admin',1),(6,'manage_log',1),(8,'manage_menu',1);
/*!40000 ALTER TABLE `permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `role`
--

DROP TABLE IF EXISTS `role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `admin_type` varchar(32) NOT NULL COMMENT '管理员类型',
  `is_using` int NOT NULL DEFAULT '1' COMMENT '标识当前角色 是否还在使用(1：使用中 0：暂停使用)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理员角色表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role`
--

LOCK TABLES `role` WRITE;
/*!40000 ALTER TABLE `role` DISABLE KEYS */;
INSERT INTO `role` VALUES (1,'admin',1),(2,'super_admin',1);
/*!40000 ALTER TABLE `role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `role_menu`
--

DROP TABLE IF EXISTS `role_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role_menu` (
  `role_id` bigint NOT NULL COMMENT '角色id',
  `menu_id` bigint NOT NULL COMMENT '菜单id',
  `is_valid` int NOT NULL DEFAULT '1' COMMENT '是否有效 1有效 0失效',
  PRIMARY KEY (`role_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色菜单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role_menu`
--

LOCK TABLES `role_menu` WRITE;
/*!40000 ALTER TABLE `role_menu` DISABLE KEYS */;
INSERT INTO `role_menu` VALUES (1,1,1),(1,2,1),(1,3,1),(1,6,1),(2,1,1),(2,2,1),(2,3,1),(2,6,1),(2,11,1),(2,12,1),(2,13,1),(2,14,1);
/*!40000 ALTER TABLE `role_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `role_permission`
--

DROP TABLE IF EXISTS `role_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `role_permission` (
  `role_id` bigint NOT NULL COMMENT '角色id',
  `permission_id` bigint NOT NULL COMMENT '权限id',
  `is_valid` int NOT NULL DEFAULT '1' COMMENT '标识 此关联是否 有效（1：有效 0 失效）',
  PRIMARY KEY (`role_id`,`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色权限关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role_permission`
--

LOCK TABLES `role_permission` WRITE;
/*!40000 ALTER TABLE `role_permission` DISABLE KEYS */;
INSERT INTO `role_permission` VALUES (1,1,1),(1,2,1),(1,3,1),(1,4,1),(2,1,1),(2,2,1),(2,3,1),(2,4,1),(2,5,1),(2,6,1),(2,8,1);
/*!40000 ALTER TABLE `role_permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_menu`
--

DROP TABLE IF EXISTS `sys_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` varchar(10) NOT NULL COMMENT '菜单名称',
  `parent_id` bigint DEFAULT NULL COMMENT '父亲id null 代表没有父亲id',
  `is_delete` int NOT NULL DEFAULT '0' COMMENT '是否删除 1删除 0未删除',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `create_by` bigint NOT NULL COMMENT '创建者 id',
  `update_time` datetime NOT NULL COMMENT '修改时间',
  `update_by` bigint NOT NULL COMMENT '修改者 id',
  `sort` int NOT NULL DEFAULT '1' COMMENT '表示顺序 值越小 顺序越靠前',
  `url` varchar(32) DEFAULT NULL COMMENT '菜单路径 如果为null 则表示其为父菜单 反之 则直接跳转',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_menu`
--

LOCK TABLES `sys_menu` WRITE;
/*!40000 ALTER TABLE `sys_menu` DISABLE KEYS */;
INSERT INTO `sys_menu` VALUES (1,'用户管理',NULL,0,'2025-03-15 11:54:31',1,'2025-03-15 11:54:53',1,1,'/userManage'),(2,'博客管理',NULL,0,'2025-03-15 11:58:55',1,'2025-03-15 11:58:59',1,2,'/blogManger'),(3,'留言管理',NULL,0,'2025-03-15 11:59:41',1,'2025-03-15 11:59:44',1,3,NULL),(4,'评论管理',3,0,'2025-03-15 13:28:52',1,'2025-03-15 13:28:51',1,1,NULL),(5,'回复管理',3,0,'2025-03-15 13:29:28',1,'2025-03-15 13:29:30',1,2,NULL),(6,'举报管理',NULL,0,'2025-03-15 13:29:53',1,'2025-03-15 13:29:57',1,4,NULL),(7,'评论举报',6,0,'2025-03-15 13:30:25',1,'2025-03-15 13:30:29',1,1,NULL),(8,'回复举报',6,0,'2025-03-15 13:31:04',1,'2025-03-15 13:31:08',1,2,NULL),(9,'博客举报',6,0,'2025-03-15 13:31:45',1,'2025-03-15 13:31:48',1,3,NULL),(10,'用户举报',6,0,'2025-03-15 13:32:17',1,'2025-03-15 13:32:20',1,4,NULL),(11,'管理员管理',NULL,0,'2025-03-15 13:33:20',1,'2025-03-15 13:33:23',1,5,NULL),(12,'日志管理',NULL,0,'2025-03-15 13:34:18',1,'2025-03-15 13:34:21',1,6,NULL),(13,'菜单管理',NULL,0,'2025-03-15 13:34:40',1,'2025-03-15 13:34:43',1,7,NULL),(14,'公告任务',NULL,0,'2025-03-19 11:06:37',1,'2025-03-19 11:06:42',1,8,NULL),(15,'公告管理',14,0,'2025-03-19 11:07:12',1,'2025-03-19 11:07:16',1,1,NULL),(16,'任务管理',14,0,'2025-03-19 11:08:02',1,'2025-03-19 11:08:05',1,2,'/taskManage');
/*!40000 ALTER TABLE `sys_menu` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-03-20 21:12:34
