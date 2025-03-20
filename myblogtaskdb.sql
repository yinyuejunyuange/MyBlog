-- MySQL dump 10.13  Distrib 8.0.36, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: my_blog_taskdb
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
-- Table structure for table `announcement`
--

DROP TABLE IF EXISTS `announcement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `announcement` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `admin_id` bigint NOT NULL COMMENT '公告对应的管理员id',
  `title` varchar(255) NOT NULL COMMENT '公告标题',
  `content` text NOT NULL COMMENT '内容',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '修改时间',
  `is_delete` int NOT NULL DEFAULT '0' COMMENT '0：未删除 1：已经删除',
  `task_id` bigint DEFAULT NULL COMMENT '创建此公告的任务id 为null 的情况下 就是管理员手动创建',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1902706014244151298 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `announcement`
--

LOCK TABLES `announcement` WRITE;
/*!40000 ALTER TABLE `announcement` DISABLE KEYS */;
INSERT INTO `announcement` VALUES (1,1,'123测试','123测试','2025-03-19 10:58:52','2025-03-19 10:58:55',0,1),(1902671444211736577,2,'测试2','测试','2025-03-20 10:39:57','2025-03-20 10:39:57',0,1902671443561619458),(1902671444798939137,2,'测试2','测试','2025-03-20 10:39:57','2025-03-20 10:39:57',0,1902671443561619458),(1902671448989048833,2,'测试2','测试','2025-03-20 10:39:58','2025-03-20 10:39:58',0,1902671443561619458),(1902671453200130049,2,'测试2','测试','2025-03-20 10:39:59','2025-03-20 10:39:59',0,1902671443561619458),(1902671457365073922,2,'测试2','测试','2025-03-20 10:40:00','2025-03-20 10:40:00',0,1902671443561619458),(1902671461584543745,2,'测试2','测试','2025-03-20 10:40:01','2025-03-20 10:40:01',0,1902671443561619458),(1902671465757876226,2,'测试2','测试','2025-03-20 10:40:02','2025-03-20 10:40:02',0,1902671443561619458),(1902671469985734658,2,'测试2','测试','2025-03-20 10:40:03','2025-03-20 10:40:03',0,1902671443561619458),(1902671474146484226,2,'测试2','测试','2025-03-20 10:40:04','2025-03-20 10:40:04',0,1902671443561619458),(1902671478353371137,2,'测试2','测试','2025-03-20 10:40:05','2025-03-20 10:40:05',0,1902671443561619458),(1902671482539286529,2,'测试2','测试','2025-03-20 10:40:06','2025-03-20 10:40:06',0,1902671443561619458),(1902671486716813313,2,'测试2','测试','2025-03-20 10:40:07','2025-03-20 10:40:07',0,1902671443561619458),(1902671490915311618,2,'测试2','测试','2025-03-20 10:40:08','2025-03-20 10:40:08',0,1902671443561619458),(1902698651797766145,2,'增加数据','qwqwerqwer','2025-03-20 12:28:04','2025-03-20 12:28:04',0,1902670728847998978),(1902698857524183041,2,'测试2','测试','2025-03-20 12:28:53','2025-03-20 12:28:53',0,1902671443561619458),(1902699434106765313,2,'测试2','测试oyyj','2025-03-20 12:31:10','2025-03-20 12:31:10',0,1902699433653780481),(1902699437571260418,2,'测试2','测试oyyj','2025-03-20 12:31:11','2025-03-20 12:31:11',0,1902699433653780481),(1902699441765564418,2,'测试2','测试oyyj','2025-03-20 12:31:12','2025-03-20 12:31:12',0,1902699433653780481),(1902699445972451329,2,'测试2','测试oyyj','2025-03-20 12:31:13','2025-03-20 12:31:13',0,1902699433653780481),(1902699450166755330,2,'测试2','测试oyyj','2025-03-20 12:31:14','2025-03-20 12:31:14',0,1902699433653780481),(1902699454348476417,2,'测试2','测试oyyj','2025-03-20 12:31:15','2025-03-20 12:31:15',0,1902699433653780481),(1902699458567946242,2,'测试2','测试oyyj','2025-03-20 12:31:16','2025-03-20 12:31:16',0,1902699433653780481),(1902699462737084417,2,'测试2','测试oyyj','2025-03-20 12:31:17','2025-03-20 12:31:17',0,1902699433653780481),(1902699466922999810,2,'测试2','测试oyyj','2025-03-20 12:31:18','2025-03-20 12:31:18',0,1902699433653780481),(1902699471138275330,2,'测试2','测试oyyj','2025-03-20 12:31:19','2025-03-20 12:31:19',0,1902699433653780481),(1902699475311607810,2,'测试2','测试oyyj','2025-03-20 12:31:20','2025-03-20 12:31:20',0,1902699433653780481),(1902699479497523202,2,'测试2','测试oyyj','2025-03-20 12:31:21','2025-03-20 12:31:21',0,1902699433653780481),(1902699491241574402,2,'测试2','测试oyyj','2025-03-20 12:31:24','2025-03-20 12:31:24',0,1902699433653780481),(1902699491505815553,2,'测试2','测试oyyj','2025-03-20 12:31:24','2025-03-20 12:31:24',0,1902699433653780481),(1902699492139155458,2,'测试2','测试oyyj','2025-03-20 12:31:24','2025-03-20 12:31:24',0,1902699433653780481),(1902699496295710721,2,'测试2','测试oyyj','2025-03-20 12:31:25','2025-03-20 12:31:25',0,1902699433653780481),(1902699500519374850,2,'测试2','测试oyyj','2025-03-20 12:31:26','2025-03-20 12:31:26',0,1902699433653780481),(1902699504747233281,2,'测试2','测试oyyj','2025-03-20 12:31:27','2025-03-20 12:31:27',0,1902699433653780481),(1902699508903788546,2,'测试2','测试oyyj','2025-03-20 12:31:28','2025-03-20 12:31:28',0,1902699433653780481),(1902699513089703938,2,'测试2','测试oyyj','2025-03-20 12:31:29','2025-03-20 12:31:29',0,1902699433653780481),(1902699517288202242,2,'测试2','测试oyyj','2025-03-20 12:31:30','2025-03-20 12:31:30',0,1902699433653780481),(1902705102461181953,2,'测试2','测试oyyjsssss','2025-03-20 12:53:42','2025-03-20 12:53:42',0,1902699433653780481),(1902705104084377601,2,'测试2','测试oyyjsssss','2025-03-20 12:53:42','2025-03-20 12:53:42',0,1902699433653780481),(1902705108270292993,2,'测试2','测试oyyjsssss','2025-03-20 12:53:43','2025-03-20 12:53:43',0,1902699433653780481),(1902705112464596994,2,'测试2','测试oyyjsssss','2025-03-20 12:53:44','2025-03-20 12:53:44',0,1902699433653780481),(1902705116717621250,2,'测试2','测试oyyjsssss','2025-03-20 12:53:45','2025-03-20 12:53:45',0,1902699433653780481),(1902705120840622081,2,'测试2','测试oyyjsssss','2025-03-20 12:53:46','2025-03-20 12:53:46',0,1902699433653780481),(1902705125034926081,2,'测试2','测试oyyjsssss','2025-03-20 12:53:47','2025-03-20 12:53:47',0,1902699433653780481),(1902705129233424386,2,'测试2','测试oyyjsssss','2025-03-20 12:53:48','2025-03-20 12:53:48',0,1902699433653780481),(1902705133461282818,2,'测试2','测试oyyjsssss','2025-03-20 12:53:49','2025-03-20 12:53:49',0,1902699433653780481),(1902705137672364033,2,'测试2','测试oyyjsssss','2025-03-20 12:53:50','2025-03-20 12:53:50',0,1902699433653780481),(1902705141824724993,2,'测试2','测试oyyjsssss','2025-03-20 12:53:51','2025-03-20 12:53:51',0,1902699433653780481),(1902705146065166337,2,'测试2','测试oyyjsssss','2025-03-20 12:53:52','2025-03-20 12:53:52',0,1902699433653780481),(1902705150255276033,2,'测试2','测试oyyjsssss','2025-03-20 12:53:53','2025-03-20 12:53:53',0,1902699433653780481),(1902705154432802818,2,'测试2','测试oyyjsssss','2025-03-20 12:53:54','2025-03-20 12:53:54',0,1902699433653780481),(1902705158618718210,2,'测试2','测试oyyjsssss','2025-03-20 12:53:55','2025-03-20 12:53:55',0,1902699433653780481),(1902705162829799426,2,'测试2','测试oyyjsssss','2025-03-20 12:53:56','2025-03-20 12:53:56',0,1902699433653780481),(1902705166990548993,2,'测试2','测试oyyjsssss','2025-03-20 12:53:57','2025-03-20 12:53:57',0,1902699433653780481),(1902705171176464385,2,'测试2','测试oyyjsssss','2025-03-20 12:53:58','2025-03-20 12:53:58',0,1902699433653780481),(1902705175374962689,2,'测试2','测试oyyjsssss','2025-03-20 12:53:59','2025-03-20 12:53:59',0,1902699433653780481),(1902705179573460994,2,'测试2','测试oyyjsssss','2025-03-20 12:54:00','2025-03-20 12:54:00',0,1902699433653780481),(1902705183797125122,2,'测试2','测试oyyjsssss','2025-03-20 12:54:01','2025-03-20 12:54:01',0,1902699433653780481),(1902705187983040514,2,'测试2','测试oyyjsssss','2025-03-20 12:54:02','2025-03-20 12:54:02',0,1902699433653780481),(1902705192173150210,2,'测试2','测试oyyjsssss','2025-03-20 12:54:03','2025-03-20 12:54:03',0,1902699433653780481),(1902705196367454209,2,'测试2','测试oyyjsssss','2025-03-20 12:54:04','2025-03-20 12:54:04',0,1902699433653780481),(1902705200553369601,2,'测试2','测试oyyjsssss','2025-03-20 12:54:05','2025-03-20 12:54:05',0,1902699433653780481),(1902705204726702082,2,'测试2','测试oyyjsssss','2025-03-20 12:54:06','2025-03-20 12:54:06',0,1902699433653780481),(1902705322913800194,2,'测试2','yyds','2025-03-20 12:54:34','2025-03-20 12:54:34',0,1902699433653780481),(1902705326420238337,2,'测试2','yyds','2025-03-20 12:54:35','2025-03-20 12:54:35',0,1902699433653780481),(1902705330568404993,2,'测试2','yyds','2025-03-20 12:54:36','2025-03-20 12:54:36',0,1902699433653780481),(1902705334754320386,2,'测试2','yyds','2025-03-20 12:54:37','2025-03-20 12:54:37',0,1902699433653780481),(1902705338969595905,2,'测试2','yyds','2025-03-20 12:54:38','2025-03-20 12:54:38',0,1902699433653780481),(1902705343142928385,2,'测试2','yyds','2025-03-20 12:54:39','2025-03-20 12:54:39',0,1902699433653780481),(1902705347341426690,2,'测试2','yyds','2025-03-20 12:54:40','2025-03-20 12:54:40',0,1902699433653780481),(1902705351531536385,2,'测试2','yyds','2025-03-20 12:54:41','2025-03-20 12:54:41',0,1902699433653780481),(1902705355734228994,2,'测试2','yyds','2025-03-20 12:54:42','2025-03-20 12:54:42',0,1902699433653780481),(1902705359920144386,2,'测试2','yyds','2025-03-20 12:54:43','2025-03-20 12:54:43',0,1902699433653780481),(1902705364110254082,2,'测试2','yyds','2025-03-20 12:54:44','2025-03-20 12:54:44',0,1902699433653780481),(1902705368308752386,2,'测试2','yyds','2025-03-20 12:54:45','2025-03-20 12:54:45',0,1902699433653780481),(1902705372503056385,2,'测试2','yyds','2025-03-20 12:54:46','2025-03-20 12:54:46',0,1902699433653780481),(1902705376701554690,2,'测试2','yyds','2025-03-20 12:54:47','2025-03-20 12:54:47',0,1902699433653780481),(1902705380916830209,2,'测试2','yyds','2025-03-20 12:54:48','2025-03-20 12:54:48',0,1902699433653780481),(1902705385081774082,2,'测试2','yyds','2025-03-20 12:54:49','2025-03-20 12:54:49',0,1902699433653780481),(1902705389284466689,2,'测试2','yyds','2025-03-20 12:54:50','2025-03-20 12:54:50',0,1902699433653780481),(1902705393466187778,2,'测试2','yyds','2025-03-20 12:54:51','2025-03-20 12:54:51',0,1902699433653780481),(1902705397668880385,2,'测试2','yyds','2025-03-20 12:54:52','2025-03-20 12:54:52',0,1902699433653780481),(1902705401867378689,2,'测试2','yyds','2025-03-20 12:54:53','2025-03-20 12:54:53',0,1902699433653780481),(1902705406057488386,2,'测试2','yyds','2025-03-20 12:54:54','2025-03-20 12:54:54',0,1902699433653780481),(1902705410247598082,2,'测试2','yyds','2025-03-20 12:54:55','2025-03-20 12:54:55',0,1902699433653780481),(1902705414450290689,2,'测试2','yyds','2025-03-20 12:54:56','2025-03-20 12:54:56',0,1902699433653780481),(1902705418640400385,2,'测试2','yyds','2025-03-20 12:54:57','2025-03-20 12:54:57',0,1902699433653780481),(1902705422834704386,2,'测试2','yyds','2025-03-20 12:54:58','2025-03-20 12:54:58',0,1902699433653780481),(1902705427070951425,2,'测试2','yyds','2025-03-20 12:54:59','2025-03-20 12:54:59',0,1902699433653780481),(1902705431273644034,2,'测试2','yyds','2025-03-20 12:55:00','2025-03-20 12:55:00',0,1902699433653780481),(1902705435413422081,2,'测试2','yyds','2025-03-20 12:55:01','2025-03-20 12:55:01',0,1902699433653780481),(1902705439607726081,2,'测试2','yyds','2025-03-20 12:55:02','2025-03-20 12:55:02',0,1902699433653780481),(1902705443835584514,2,'测试2','yyds','2025-03-20 12:55:03','2025-03-20 12:55:03',0,1902699433653780481),(1902705448021499906,2,'测试2','yyds','2025-03-20 12:55:04','2025-03-20 12:55:04',0,1902699433653780481),(1902705452207415297,2,'测试2','yyds','2025-03-20 12:55:05','2025-03-20 12:55:05',0,1902699433653780481),(1902705456384942081,2,'测试2','yyds','2025-03-20 12:55:06','2025-03-20 12:55:06',0,1902699433653780481),(1902705460629577730,2,'测试2','yyds','2025-03-20 12:55:07','2025-03-20 12:55:07',0,1902699433653780481),(1902705464832270337,2,'测试2','yyds','2025-03-20 12:55:08','2025-03-20 12:55:08',0,1902699433653780481),(1902705468993019906,2,'测试2','yyds','2025-03-20 12:55:09','2025-03-20 12:55:09',0,1902699433653780481),(1902705473183129602,2,'测试2','yyds','2025-03-20 12:55:10','2025-03-20 12:55:10',0,1902699433653780481),(1902705477356462082,2,'测试2','yyds','2025-03-20 12:55:11','2025-03-20 12:55:11',0,1902699433653780481),(1902705481550766081,2,'测试2','yyds','2025-03-20 12:55:12','2025-03-20 12:55:12',0,1902699433653780481),(1902705988843446274,2,'测试2','yyds','2025-03-20 12:57:13','2025-03-20 12:57:13',0,1902699433653780481),(1902705989048967169,2,'测试2','yyds','2025-03-20 12:57:13','2025-03-20 12:57:13',0,1902699433653780481),(1902705993268436993,2,'测试2','yyds','2025-03-20 12:57:14','2025-03-20 12:57:14',0,1902699433653780481),(1902705997483712514,2,'测试2','yyds','2025-03-20 12:57:15','2025-03-20 12:57:15',0,1902699433653780481),(1902706001703182337,2,'测试2','yyds','2025-03-20 12:57:16','2025-03-20 12:57:16',0,1902699433653780481),(1902706005893292033,2,'测试2','yyds','2025-03-20 12:57:17','2025-03-20 12:57:17',0,1902699433653780481),(1902706010091790338,2,'测试2','yyds','2025-03-20 12:57:18','2025-03-20 12:57:18',0,1902699433653780481),(1902706014244151297,2,'测试2','yyds','2025-03-20 12:57:19','2025-03-20 12:57:19',0,1902699433653780481);
/*!40000 ALTER TABLE `announcement` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `announcement_task`
--

DROP TABLE IF EXISTS `announcement_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `announcement_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
  `admin_id` bigint NOT NULL COMMENT '创建任务的管理员id',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '修改时间',
  `frequency` varchar(64) NOT NULL COMMENT '执行频率 CRON表达式',
  `is_delete` int NOT NULL DEFAULT '0' COMMENT '0:未删除 1：已经删除',
  `task_name` varchar(64) NOT NULL COMMENT '任务名称',
  `title` varchar(255) NOT NULL COMMENT '公告标题',
  `content` text NOT NULL COMMENT '内容',
  `update_by` bigint DEFAULT NULL COMMENT '修改者id',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1902699433653780482 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `announcement_task`
--

LOCK TABLES `announcement_task` WRITE;
/*!40000 ALTER TABLE `announcement_task` DISABLE KEYS */;
INSERT INTO `announcement_task` VALUES (1902353590291542017,2,'2025-03-19 13:36:55','2025-03-19 13:36:55','* * 21 19 3 3 *',0,'123','1231','123123',NULL),(1902354043674832897,2,'2025-03-19 13:38:43','2025-03-19 13:38:43','* * 21 19 3 ? *',0,'123','123','123',NULL),(1902670728847998978,2,'2025-03-20 10:37:06','2025-03-20 10:37:06','* * 18 20 3 ? 2025',0,'增加数据','增加数据','qwqwerqwer',NULL),(1902671443561619458,2,'2025-03-20 10:39:57','2025-03-20 10:39:57','* * 18 20 3 ? 2025',0,'测试2','测试2','测试',NULL),(1902699433653780481,2,'2025-03-20 12:31:10','2025-03-20 12:54:23','* * 20 20 3 ? 2025',0,'oyyj','测试2','yyds',2);
/*!40000 ALTER TABLE `announcement_task` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `announcement_user`
--

DROP TABLE IF EXISTS `announcement_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `announcement_user` (
  `announcement_id` bigint NOT NULL COMMENT '公告id',
  `user_id` bigint NOT NULL COMMENT '用户id',
  PRIMARY KEY (`announcement_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户公告关联表 记录用户查看了哪些公告';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `announcement_user`
--

LOCK TABLES `announcement_user` WRITE;
/*!40000 ALTER TABLE `announcement_user` DISABLE KEYS */;
/*!40000 ALTER TABLE `announcement_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_blob_triggers`
--

DROP TABLE IF EXISTS `qrtz_blob_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_blob_triggers` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(190) NOT NULL,
  `TRIGGER_GROUP` varchar(190) NOT NULL,
  `BLOB_DATA` blob,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  KEY `SCHED_NAME` (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  CONSTRAINT `qrtz_blob_triggers_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `qrtz_triggers` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_blob_triggers`
--

LOCK TABLES `qrtz_blob_triggers` WRITE;
/*!40000 ALTER TABLE `qrtz_blob_triggers` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_blob_triggers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_calendars`
--

DROP TABLE IF EXISTS `qrtz_calendars`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_calendars` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `CALENDAR_NAME` varchar(190) NOT NULL,
  `CALENDAR` blob NOT NULL,
  PRIMARY KEY (`SCHED_NAME`,`CALENDAR_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_calendars`
--

LOCK TABLES `qrtz_calendars` WRITE;
/*!40000 ALTER TABLE `qrtz_calendars` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_calendars` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_cron_triggers`
--

DROP TABLE IF EXISTS `qrtz_cron_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_cron_triggers` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(190) NOT NULL,
  `TRIGGER_GROUP` varchar(190) NOT NULL,
  `CRON_EXPRESSION` varchar(120) NOT NULL,
  `TIME_ZONE_ID` varchar(80) DEFAULT NULL,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  CONSTRAINT `qrtz_cron_triggers_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `qrtz_triggers` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_cron_triggers`
--

LOCK TABLES `qrtz_cron_triggers` WRITE;
/*!40000 ALTER TABLE `qrtz_cron_triggers` DISABLE KEYS */;
INSERT INTO `qrtz_cron_triggers` VALUES ('quartzScheduler','1902699433653780481_trigger','DEFAULT','* * 20 20 3 ? 2025','Asia/Shanghai');
/*!40000 ALTER TABLE `qrtz_cron_triggers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_fired_triggers`
--

DROP TABLE IF EXISTS `qrtz_fired_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_fired_triggers` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `ENTRY_ID` varchar(95) NOT NULL,
  `TRIGGER_NAME` varchar(190) NOT NULL,
  `TRIGGER_GROUP` varchar(190) NOT NULL,
  `INSTANCE_NAME` varchar(190) NOT NULL,
  `FIRED_TIME` bigint NOT NULL,
  `SCHED_TIME` bigint NOT NULL,
  `PRIORITY` int NOT NULL,
  `STATE` varchar(16) NOT NULL,
  `JOB_NAME` varchar(190) DEFAULT NULL,
  `JOB_GROUP` varchar(190) DEFAULT NULL,
  `IS_NONCONCURRENT` varchar(1) DEFAULT NULL,
  `REQUESTS_RECOVERY` varchar(1) DEFAULT NULL,
  PRIMARY KEY (`SCHED_NAME`,`ENTRY_ID`),
  KEY `IDX_QRTZ_FT_TRIG_INST_NAME` (`SCHED_NAME`,`INSTANCE_NAME`),
  KEY `IDX_QRTZ_FT_INST_JOB_REQ_RCVRY` (`SCHED_NAME`,`INSTANCE_NAME`,`REQUESTS_RECOVERY`),
  KEY `IDX_QRTZ_FT_J_G` (`SCHED_NAME`,`JOB_NAME`,`JOB_GROUP`),
  KEY `IDX_QRTZ_FT_JG` (`SCHED_NAME`,`JOB_GROUP`),
  KEY `IDX_QRTZ_FT_T_G` (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  KEY `IDX_QRTZ_FT_TG` (`SCHED_NAME`,`TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_fired_triggers`
--

LOCK TABLES `qrtz_fired_triggers` WRITE;
/*!40000 ALTER TABLE `qrtz_fired_triggers` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_fired_triggers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_job_details`
--

DROP TABLE IF EXISTS `qrtz_job_details`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_job_details` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `JOB_NAME` varchar(190) NOT NULL,
  `JOB_GROUP` varchar(190) NOT NULL,
  `DESCRIPTION` varchar(250) DEFAULT NULL,
  `JOB_CLASS_NAME` varchar(250) NOT NULL,
  `IS_DURABLE` varchar(1) NOT NULL,
  `IS_NONCONCURRENT` varchar(1) NOT NULL,
  `IS_UPDATE_DATA` varchar(1) NOT NULL,
  `REQUESTS_RECOVERY` varchar(1) NOT NULL,
  `JOB_DATA` blob,
  PRIMARY KEY (`SCHED_NAME`,`JOB_NAME`,`JOB_GROUP`),
  KEY `IDX_QRTZ_J_REQ_RECOVERY` (`SCHED_NAME`,`REQUESTS_RECOVERY`),
  KEY `IDX_QRTZ_J_GRP` (`SCHED_NAME`,`JOB_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_job_details`
--

LOCK TABLES `qrtz_job_details` WRITE;
/*!40000 ALTER TABLE `qrtz_job_details` DISABLE KEYS */;
INSERT INTO `qrtz_job_details` VALUES ('quartzScheduler','1902670728847998978','DEFAULT',NULL,'org.oyyj.taskservice.job.AnnouncementJob','1','1','1','0',_binary '�\�\0sr\0org.quartz.JobDataMap���迩�\�\0\0xr\0&org.quartz.utils.StringKeyDirtyFlagMap�\�\��\�](\0Z\0allowsTransientDataxr\0org.quartz.utils.DirtyFlagMap\�.�(v\n\�\0Z\0dirtyL\0mapt\0Ljava/util/Map;xpsr\0java.util.HashMap\��\�`\�\0F\0\nloadFactorI\0	thresholdxp?@\0\0\0\0\0w\0\0\0\0\0\0t\0\ncreateTimet\0nullt\0adminIdt\02t\0\nupdateTimeq\0~\0t\0titlet\0增加数据t\0taskIdsr\0java.lang.Long;�\�̏#\�\0J\0valuexr\0java.lang.Number����\��\0\0xp\Zg�\�u��t\0contentt\0\nqwqwerqwerx\0'),('quartzScheduler','1902671443561619458','DEFAULT',NULL,'org.oyyj.taskservice.job.AnnouncementJob','1','1','1','0',_binary '�\�\0sr\0org.quartz.JobDataMap���迩�\�\0\0xr\0&org.quartz.utils.StringKeyDirtyFlagMap�\�\��\�](\0Z\0allowsTransientDataxr\0org.quartz.utils.DirtyFlagMap\�.�(v\n\�\0Z\0dirtyL\0mapt\0Ljava/util/Map;xpsr\0java.util.HashMap\��\�`\�\0F\0\nloadFactorI\0	thresholdxp?@\0\0\0\0\0w\0\0\0\0\0\0t\0\ncreateTimet\0nullt\0adminIdt\02t\0\nupdateTimeq\0~\0t\0titlet\0测试2t\0taskIdsr\0java.lang.Long;�\�̏#\�\0J\0valuexr\0java.lang.Number����\��\0\0xp\Zg��\�\� t\0contentt\0测试x\0'),('quartzScheduler','1902699433653780481','DEFAULT',NULL,'org.oyyj.taskservice.job.AnnouncementJob','1','1','1','0',_binary '�\�\0sr\0org.quartz.JobDataMap���迩�\�\0\0xr\0&org.quartz.utils.StringKeyDirtyFlagMap�\�\��\�](\0Z\0allowsTransientDataxr\0org.quartz.utils.DirtyFlagMap\�.�(v\n\�\0Z\0dirtyL\0mapt\0Ljava/util/Map;xpsr\0java.util.HashMap\��\�`\�\0F\0\nloadFactorI\0	thresholdxp?@\0\0\0\0\0w\0\0\0\0\0\0t\0\ncreateTimet\0nullt\0adminIdt\02t\0\nupdateTimet\0nullt\0titlet\0测试2t\0taskIdt\01902699433653780481t\0contentt\0yydsx\0');
/*!40000 ALTER TABLE `qrtz_job_details` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_locks`
--

DROP TABLE IF EXISTS `qrtz_locks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_locks` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `LOCK_NAME` varchar(40) NOT NULL,
  PRIMARY KEY (`SCHED_NAME`,`LOCK_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_locks`
--

LOCK TABLES `qrtz_locks` WRITE;
/*!40000 ALTER TABLE `qrtz_locks` DISABLE KEYS */;
INSERT INTO `qrtz_locks` VALUES ('quartzScheduler','STATE_ACCESS'),('quartzScheduler','TRIGGER_ACCESS');
/*!40000 ALTER TABLE `qrtz_locks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_paused_trigger_grps`
--

DROP TABLE IF EXISTS `qrtz_paused_trigger_grps`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_paused_trigger_grps` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_GROUP` varchar(190) NOT NULL,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_paused_trigger_grps`
--

LOCK TABLES `qrtz_paused_trigger_grps` WRITE;
/*!40000 ALTER TABLE `qrtz_paused_trigger_grps` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_paused_trigger_grps` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_scheduler_state`
--

DROP TABLE IF EXISTS `qrtz_scheduler_state`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_scheduler_state` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `INSTANCE_NAME` varchar(190) NOT NULL,
  `LAST_CHECKIN_TIME` bigint NOT NULL,
  `CHECKIN_INTERVAL` bigint NOT NULL,
  PRIMARY KEY (`SCHED_NAME`,`INSTANCE_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_scheduler_state`
--

LOCK TABLES `qrtz_scheduler_state` WRITE;
/*!40000 ALTER TABLE `qrtz_scheduler_state` DISABLE KEYS */;
INSERT INTO `qrtz_scheduler_state` VALUES ('quartzScheduler','DESKTOP-ABU0T191742474792305',1742475492364,7500);
/*!40000 ALTER TABLE `qrtz_scheduler_state` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_simple_triggers`
--

DROP TABLE IF EXISTS `qrtz_simple_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_simple_triggers` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(190) NOT NULL,
  `TRIGGER_GROUP` varchar(190) NOT NULL,
  `REPEAT_COUNT` bigint NOT NULL,
  `REPEAT_INTERVAL` bigint NOT NULL,
  `TIMES_TRIGGERED` bigint NOT NULL,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  CONSTRAINT `qrtz_simple_triggers_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `qrtz_triggers` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_simple_triggers`
--

LOCK TABLES `qrtz_simple_triggers` WRITE;
/*!40000 ALTER TABLE `qrtz_simple_triggers` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_simple_triggers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_simprop_triggers`
--

DROP TABLE IF EXISTS `qrtz_simprop_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_simprop_triggers` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(190) NOT NULL,
  `TRIGGER_GROUP` varchar(190) NOT NULL,
  `STR_PROP_1` varchar(512) DEFAULT NULL,
  `STR_PROP_2` varchar(512) DEFAULT NULL,
  `STR_PROP_3` varchar(512) DEFAULT NULL,
  `INT_PROP_1` int DEFAULT NULL,
  `INT_PROP_2` int DEFAULT NULL,
  `LONG_PROP_1` bigint DEFAULT NULL,
  `LONG_PROP_2` bigint DEFAULT NULL,
  `DEC_PROP_1` decimal(13,4) DEFAULT NULL,
  `DEC_PROP_2` decimal(13,4) DEFAULT NULL,
  `BOOL_PROP_1` varchar(1) DEFAULT NULL,
  `BOOL_PROP_2` varchar(1) DEFAULT NULL,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  CONSTRAINT `qrtz_simprop_triggers_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `qrtz_triggers` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_simprop_triggers`
--

LOCK TABLES `qrtz_simprop_triggers` WRITE;
/*!40000 ALTER TABLE `qrtz_simprop_triggers` DISABLE KEYS */;
/*!40000 ALTER TABLE `qrtz_simprop_triggers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `qrtz_triggers`
--

DROP TABLE IF EXISTS `qrtz_triggers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `qrtz_triggers` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(190) NOT NULL,
  `TRIGGER_GROUP` varchar(190) NOT NULL,
  `JOB_NAME` varchar(190) NOT NULL,
  `JOB_GROUP` varchar(190) NOT NULL,
  `DESCRIPTION` varchar(250) DEFAULT NULL,
  `NEXT_FIRE_TIME` bigint DEFAULT NULL,
  `PREV_FIRE_TIME` bigint DEFAULT NULL,
  `PRIORITY` int DEFAULT NULL,
  `TRIGGER_STATE` varchar(16) NOT NULL,
  `TRIGGER_TYPE` varchar(8) NOT NULL,
  `START_TIME` bigint NOT NULL,
  `END_TIME` bigint DEFAULT NULL,
  `CALENDAR_NAME` varchar(190) DEFAULT NULL,
  `MISFIRE_INSTR` smallint DEFAULT NULL,
  `JOB_DATA` blob,
  PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  KEY `IDX_QRTZ_T_J` (`SCHED_NAME`,`JOB_NAME`,`JOB_GROUP`),
  KEY `IDX_QRTZ_T_JG` (`SCHED_NAME`,`JOB_GROUP`),
  KEY `IDX_QRTZ_T_C` (`SCHED_NAME`,`CALENDAR_NAME`),
  KEY `IDX_QRTZ_T_G` (`SCHED_NAME`,`TRIGGER_GROUP`),
  KEY `IDX_QRTZ_T_STATE` (`SCHED_NAME`,`TRIGGER_STATE`),
  KEY `IDX_QRTZ_T_N_STATE` (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`,`TRIGGER_STATE`),
  KEY `IDX_QRTZ_T_N_G_STATE` (`SCHED_NAME`,`TRIGGER_GROUP`,`TRIGGER_STATE`),
  KEY `IDX_QRTZ_T_NEXT_FIRE_TIME` (`SCHED_NAME`,`NEXT_FIRE_TIME`),
  KEY `IDX_QRTZ_T_NFT_ST` (`SCHED_NAME`,`TRIGGER_STATE`,`NEXT_FIRE_TIME`),
  KEY `IDX_QRTZ_T_NFT_MISFIRE` (`SCHED_NAME`,`MISFIRE_INSTR`,`NEXT_FIRE_TIME`),
  KEY `IDX_QRTZ_T_NFT_ST_MISFIRE` (`SCHED_NAME`,`MISFIRE_INSTR`,`NEXT_FIRE_TIME`,`TRIGGER_STATE`),
  KEY `IDX_QRTZ_T_NFT_ST_MISFIRE_GRP` (`SCHED_NAME`,`MISFIRE_INSTR`,`NEXT_FIRE_TIME`,`TRIGGER_GROUP`,`TRIGGER_STATE`),
  CONSTRAINT `qrtz_triggers_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`) REFERENCES `qrtz_job_details` (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qrtz_triggers`
--

LOCK TABLES `qrtz_triggers` WRITE;
/*!40000 ALTER TABLE `qrtz_triggers` DISABLE KEYS */;
INSERT INTO `qrtz_triggers` VALUES ('quartzScheduler','1902699433653780481_trigger','DEFAULT','1902699433653780481','DEFAULT',NULL,1742475440000,1742475439000,5,'PAUSED','CRON',1742473870000,0,NULL,0,'');
/*!40000 ALTER TABLE `qrtz_triggers` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-03-20 21:12:04
