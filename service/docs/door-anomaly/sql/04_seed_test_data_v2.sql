-- ========== 仓门配置 ==========
DELETE FROM ai_event_process_record;
DELETE FROM ai_event_evidence;
DELETE FROM ai_anomaly_event;
DELETE FROM ai_algorithm_task;
DELETE FROM ai_camera_mapping;
DELETE FROM ai_door_config;

INSERT INTO ai_door_config (id,door_code,door_name,warehouse_id,warehouse_name,region_id,org_id,camera_id,camera_code,device_status,door_physical_status,alarm_status,enabled,sort_no,last_heartbeat_at,latest_event_at,created_by,created_at,updated_by,updated_at,remark) VALUES
(1001,'D001','1号仓东门',9001,'一号粮库','000000',NULL,'C001','CAM-001','ONLINE','CLOSED','NORMAL',true,1,NOW()-INTERVAL '2 minutes',NULL,'test',NOW(),'test',NOW(),'正常'),
(1002,'D002','1号仓西门',9001,'一号粮库','000000',NULL,'C002','CAM-002','ONLINE','OPEN','NORMAL',true,2,NOW()-INTERVAL '3 minutes',NULL,'test',NOW(),'test',NOW(),'正常'),
(1003,'D003','2号仓东门',9002,'二号粮库','000000',NULL,'C003','CAM-003','ONLINE','CLOSED','NORMAL',true,3,NOW()-INTERVAL '1 minutes',NULL,'test',NOW(),'test',NOW(),'正常'),
(1004,'D004','2号仓西门',9002,'二号粮库','000000',NULL,'C004','CAM-004','ONLINE','CLOSED','NORMAL',true,4,NOW()-INTERVAL '5 minutes',NULL,'test',NOW(),'test',NOW(),'正常'),
(1005,'D005','3号仓南门',9003,'三号粮库','000000',NULL,'C005','CAM-005','ONLINE','CLOSED','NORMAL',true,5,NOW()-INTERVAL '4 minutes',NULL,'test',NOW(),'test',NOW(),'正常'),
(1006,'D006','3号仓北门',9003,'三号粮库','000000',NULL,'C006','CAM-006','ONLINE','OPEN','ACTIVE',true,6,NOW()-INTERVAL '1 minutes',NULL,'test',NOW(),'test',NOW(),'告警中'),
(1007,'D007','4号仓东门',9004,'散粮库','000000',NULL,'C007','CAM-007','ONLINE','OPEN','ACTIVE',true,7,NOW()-INTERVAL '2 minutes',NULL,'test',NOW(),'test',NOW(),'告警中'),
(1008,'D008','4号仓西门',9004,'散粮库','000000',NULL,'C008','CAM-008','ONLINE','CLOSED','RECOVERED',true,8,NOW()-INTERVAL '6 minutes',NULL,'test',NOW(),'test',NOW(),'已恢复'),
(1009,'D009','5号仓大门',9001,'一号粮库','000000',NULL,'C009','CAM-009','ONLINE','CLOSED','NORMAL',true,9,NOW()-INTERVAL '1 minutes',NULL,'test',NOW(),'test',NOW(),'正常'),
(1010,'D010','6号仓东门',9002,'二号粮库','000000',NULL,'C010','CAM-010','ONLINE','CLOSED','NORMAL',true,10,NOW()-INTERVAL '3 minutes',NULL,'test',NOW(),'test',NOW(),'正常'),
(1011,'D011','6号仓西门',9002,'二号粮库','000000',NULL,'C011','CAM-011','ONLINE','CLOSED','NORMAL',true,11,NOW()-INTERVAL '2 minutes',NULL,'test',NOW(),'test',NOW(),'正常'),
(1012,'D012','7号仓北门',9003,'三号粮库','000000',NULL,'C012','CAM-012','ONLINE','CLOSED','NORMAL',true,12,NOW()-INTERVAL '4 minutes',NULL,'test',NOW(),'test',NOW(),'正常'),
(1013,'D013','8号仓东门',9004,'散粮库','000000',NULL,'C013','CAM-013','UNKNOWN','UNKNOWN','NORMAL',true,13,NULL,NULL,'test',NOW(),'test',NOW(),'状态未知'),
(1014,'D014','8号仓西门',9004,'散粮库','000000',NULL,'C014','CAM-014','OFFLINE','UNKNOWN','NORMAL',true,14,NOW()-INTERVAL '45 minutes',NULL,'test',NOW(),'test',NOW(),'离线'),
(1015,'D015','9号仓南门',9001,'一号粮库','000000',NULL,'C015','CAM-015','OFFLINE','UNKNOWN','NORMAL',true,15,NOW()-INTERVAL '120 minutes',NULL,'test',NOW(),'test',NOW(),'离线'),
(1016,'D016','散粮仓西门',9004,'散粮库','000000',NULL,'C016','CAM-016','ONLINE','CLOSED','NORMAL',true,16,NOW()-INTERVAL '2 minutes',NULL,'test',NOW(),'test',NOW(),'正常');

-- ========== 摄像头映射 ==========
INSERT INTO ai_camera_mapping (id,door_id,camera_id,camera_code,camera_name,device_code,channel_no,supported_protocols,enabled,created_by,created_at,updated_by,updated_at) VALUES
(2001,1001,'C001','CAM-001','1号仓东门摄像头','DEV-001','CH-01','HLS,MP4',true,'test',NOW(),'test',NOW()),
(2002,1002,'C002','CAM-002','1号仓西门摄像头','DEV-001','CH-02','HLS,MP4',true,'test',NOW(),'test',NOW()),
(2003,1003,'C003','CAM-003','2号仓东门摄像头','DEV-002','CH-01','HLS,MP4',true,'test',NOW(),'test',NOW()),
(2004,1004,'C004','CAM-004','2号仓西门摄像头','DEV-002','CH-02','HLS,MP4',true,'test',NOW(),'test',NOW()),
(2005,1005,'C005','CAM-005','3号仓南门摄像头','DEV-003','CH-01','HLS,MP4',true,'test',NOW(),'test',NOW()),
(2006,1006,'C006','CAM-006','3号仓北门摄像头','DEV-003','CH-02','HLS,MP4',true,'test',NOW(),'test',NOW()),
(2007,1007,'C007','CAM-007','4号仓东门摄像头','DEV-004','CH-01','HLS,MP4',true,'test',NOW(),'test',NOW()),
(2008,1008,'C008','CAM-008','4号仓西门摄像头','DEV-004','CH-02','HLS,MP4',true,'test',NOW(),'test',NOW()),
(2009,1009,'C009','CAM-009','5号仓大门摄像头','DEV-005','CH-01','HLS,MP4',true,'test',NOW(),'test',NOW()),
(2010,1010,'C010','CAM-010','6号仓东门摄像头','DEV-005','CH-02','HLS,MP4',true,'test',NOW(),'test',NOW()),
(2011,1011,'C011','CAM-011','6号仓西门摄像头','DEV-006','CH-01','HLS,MP4',true,'test',NOW(),'test',NOW()),
(2012,1012,'C012','CAM-012','7号仓北门摄像头','DEV-006','CH-02','HLS,MP4',true,'test',NOW(),'test',NOW()),
(2013,1013,'C013','CAM-013','8号仓东门摄像头','DEV-007','CH-01','HLS,MP4',true,'test',NOW(),'test',NOW()),
(2014,1014,'C014','CAM-014','8号仓西门摄像头','DEV-007','CH-02','HLS,MP4',true,'test',NOW(),'test',NOW()),
(2015,1015,'C015','CAM-015','9号仓南门摄像头','DEV-008','CH-01','HLS,MP4',true,'test',NOW(),'test',NOW()),
(2016,1016,'C016','CAM-016','散粮仓西门摄像头','DEV-008','CH-02','HLS,MP4',true,'test',NOW(),'test',NOW());

-- ========== 算法任务 ==========
INSERT INTO ai_algorithm_task (id,task_no,candidate_event_id,door_id,camera_id,warehouse_id,task_type,media_type,media_refs,normal_reference_ref,roi,execution_status,conclusion,anomaly_type,confidence,model_name,model_version,algorithm_version,config_version,confidence_threshold,media_digest,started_at,finished_at,inference_duration_ms,detections,evidence_refs,raw_result_ref,error_code,error_message,retry_count,trace_id,is_simulated,created_by,created_at,updated_by,updated_at) VALUES
(3001,'AT-0001',NULL,1006,'C006',9003,'DOOR_ANOMALY_DETECT','DUAL_IMAGE','["mock://media/1006/normal","mock://media/1006/abnormal"]','mock://media/1006/normal','[120,80,460,420]','SUCCEEDED','ANOMALY','ILLEGAL_OPEN',0.9231,'door-anomaly-v1','1.2.3','algo-2.0.1','cfg-20261001',0.75,'sha256-mock-1006',NOW()-INTERVAL '3 minutes',NOW()-INTERVAL '2 minutes',150,'[{"category":"door_panel","confidence":0.9231,"bbox":"[120,80,460,420]","trackId":"track-0001"}]','["mock://evidence/1006/normal","mock://evidence/1006/abnormal"]','mock://task/AT-0001/raw',NULL,NULL,0,'TRACE-0001',false,'test',NOW(),'test',NOW()),
(3002,'AT-0002',NULL,1003,'C003',9002,'DOOR_ANOMALY_DETECT','DUAL_IMAGE','["mock://media/1003/normal","mock://media/1003/abnormal"]','mock://media/1003/normal','[120,80,460,420]','SUCCEEDED','NORMAL','OTHER',0.8812,'door-anomaly-v1','1.2.3','algo-2.0.1','cfg-20261001',0.75,'sha256-mock-1003',NOW()-INTERVAL '8 minutes',NOW()-INTERVAL '7 minutes',187,'[{"category":"door_panel","confidence":0.8812,"bbox":"[120,80,460,420]","trackId":"track-0002"}]','["mock://evidence/1003/normal","mock://evidence/1003/abnormal"]','mock://task/AT-0002/raw',NULL,NULL,0,'TRACE-0002',false,'test',NOW(),'test',NOW()),
(3003,'AT-0003',NULL,1007,'C007',9004,'DOOR_ANOMALY_DETECT','DUAL_IMAGE','["mock://media/1007/normal","mock://media/1007/abnormal"]','mock://media/1007/normal','[120,80,460,420]','SUCCEEDED','UNCERTAIN','ABNORMAL_MOVE',NULL,'door-anomaly-v1','1.2.3','algo-2.0.1','cfg-20261001',0.75,'sha256-mock-1007',NOW()-INTERVAL '12 minutes',NOW()-INTERVAL '11 minutes',224,'[{"category":"door_panel","confidence":null,"bbox":"[120,80,460,420]","trackId":"track-0003"}]','["mock://evidence/1007/normal","mock://evidence/1007/abnormal"]','mock://task/AT-0003/raw',NULL,NULL,0,'TRACE-0003',false,'test',NOW(),'test',NOW()),
(3004,'AT-0004',NULL,1009,'C009',9001,'DOOR_ANOMALY_DETECT','DUAL_IMAGE','["mock://media/1009/normal"]',NULL,NULL,'FAILED',NULL,NULL,NULL,NULL,NULL,NULL,'cfg-20261001',0.75,'sha256-mock-1009',NOW()-INTERVAL '20 minutes',NOW()-INTERVAL '19 minutes',5000,NULL,NULL,NULL,'ALGO_INTERNAL_ERROR','算法推理异常',1,'TRACE-0004',false,'test',NOW(),'test',NOW()),
(3005,'AT-0005',NULL,1010,'C010',9002,'DOOR_ANOMALY_DETECT','DUAL_IMAGE','["mock://media/1010/normal"]',NULL,NULL,'TIMEOUT',NULL,NULL,NULL,NULL,NULL,NULL,'cfg-20261001',0.75,'sha256-mock-1010',NOW()-INTERVAL '25 minutes',NOW()-INTERVAL '24 minutes',120000,NULL,NULL,NULL,'ALGO_TIMEOUT','算法推理超时',1,'TRACE-0005',false,'test',NOW(),'test',NOW()),
(3006,'AT-0006',NULL,1014,'C014',9004,'DOOR_ANOMALY_DETECT',NULL,NULL,NULL,NULL,'SKIPPED',NULL,NULL,NULL,NULL,NULL,NULL,'cfg-20261001',0.75,'sha256-mock-1014',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,'TRACE-0006',false,'test',NOW(),'test',NOW()),
(3007,'AT-0007',NULL,1001,'C001',9001,'DOOR_ANOMALY_DETECT','DUAL_IMAGE','["mock://media/1001/normal","mock://media/1001/abnormal"]','mock://media/1001/normal','[120,80,460,420]','MOCK','ANOMALY','ILLEGAL_OPEN',0.7788,'mock-door-detector','mock-1.0.0','mock-adapter-1.0.0','cfg-20261001',0.75,'sha256-mock-1001',NOW()-INTERVAL '35 minutes',NOW()-INTERVAL '34 minutes',372,'[{"category":"door_panel","confidence":0.7788,"bbox":"[120,80,460,420]","trackId":"track-0007"}]','["mock://evidence/1001/normal","mock://evidence/1001/abnormal"]','mock://task/AT-0007/raw',NULL,NULL,0,'TRACE-0007',true,'test',NOW(),'test',NOW()),
(3008,'AT-0008',NULL,1011,'C011',9002,'DOOR_ANOMALY_DETECT',NULL,NULL,NULL,NULL,'QUEUED',NULL,NULL,NULL,NULL,NULL,NULL,'cfg-20261001',0.75,'sha256-mock-1011',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,'TRACE-0008',false,'test',NOW(),'test',NOW()),
(3009,'AT-0009',NULL,1012,'C012',9003,'DOOR_ANOMALY_DETECT',NULL,NULL,NULL,NULL,'RUNNING',NULL,NULL,NULL,NULL,NULL,NULL,'cfg-20261001',0.75,'sha256-mock-1012',NOW()-INTERVAL '1 minutes',NULL,NULL,NULL,NULL,NULL,NULL,NULL,0,'TRACE-0009',false,'test',NOW(),'test',NOW()),
(3010,'AT-0010',NULL,1002,'C002',9001,'DOOR_ANOMALY_DETECT','DUAL_IMAGE','["mock://media/1002/normal","mock://media/1002/abnormal"]','mock://media/1002/normal','[120,80,460,420]','SUCCEEDED','ANOMALY','LONG_TIME_OPEN',0.9567,'door-anomaly-v1','1.2.3','algo-2.0.1','cfg-20261001',0.75,'sha256-mock-1002',NOW()-INTERVAL '180 minutes',NOW()-INTERVAL '179 minutes',483,'[{"category":"door_panel","confidence":0.9567,"bbox":"[120,80,460,420]","trackId":"track-0010"}]','["mock://evidence/1002/normal","mock://evidence/1002/abnormal"]','mock://task/AT-0010/raw',NULL,NULL,0,'TRACE-0010',false,'test',NOW(),'test',NOW()),
(3011,'AT-0011',NULL,1013,'C013',9004,'DOOR_ANOMALY_DETECT',NULL,'["mock://media/1013/normal","mock://media/1013/abnormal"]','mock://media/1013/normal','[120,80,460,420]','SUCCEEDED','UNCERTAIN','OTHER',NULL,'door-anomaly-v1','1.2.3','algo-2.0.1','cfg-20261001',0.75,'sha256-mock-1013',NOW()-INTERVAL '50 minutes',NOW()-INTERVAL '49 minutes',520,'[{"category":"door_panel","confidence":null,"bbox":"[120,80,460,420]","trackId":"track-0011"}]','["mock://evidence/1013/normal","mock://evidence/1013/abnormal"]','mock://task/AT-0011/raw',NULL,NULL,0,'TRACE-0011',false,'test',NOW(),'test',NOW()),
(3012,'AT-0012',NULL,1005,'C005',9003,'DOOR_ANOMALY_DETECT',NULL,'["mock://media/1005/normal","mock://media/1005/abnormal"]','mock://media/1005/normal','[120,80,460,420]','SUCCEEDED','ANOMALY','ILLEGAL_OPEN',0.8123,'door-anomaly-v1','1.2.3','algo-2.0.1','cfg-20261001',0.75,'sha256-mock-1005',NOW()-INTERVAL '10080 minutes',NOW()-INTERVAL '10079 minutes',157,'[{"category":"door_panel","confidence":0.8123,"bbox":"[120,80,460,420]","trackId":"track-0012"}]','["mock://evidence/1005/normal","mock://evidence/1005/abnormal"]','mock://task/AT-0012/raw',NULL,NULL,0,'TRACE-0012',false,'test',NOW(),'test',NOW()),
(3013,'AT-0013',NULL,1004,'C004',9002,'DOOR_ANOMALY_DETECT','DUAL_IMAGE','["mock://media/1004/normal","mock://media/1004/abnormal"]','mock://media/1004/normal','[120,80,460,420]','SUCCEEDED','ANOMALY','ABNORMAL_MOVE',0.5123,'door-anomaly-v1','1.2.3','algo-2.0.1','cfg-20261001',0.75,'sha256-mock-1004',NOW()-INTERVAL '15 minutes',NOW()-INTERVAL '14 minutes',194,'[{"category":"door_panel","confidence":0.5123,"bbox":"[120,80,460,420]","trackId":"track-0013"}]','["mock://evidence/1004/normal","mock://evidence/1004/abnormal"]','mock://task/AT-0013/raw',NULL,NULL,0,'TRACE-0013',false,'test',NOW(),'test',NOW()),
(3014,'AT-0014',NULL,1008,'C008',9004,'DOOR_ANOMALY_DETECT','DUAL_IMAGE','["mock://media/1008/normal","mock://media/1008/abnormal"]','mock://media/1008/normal','[120,80,460,420]','SUCCEEDED','ANOMALY','ILLEGAL_OPEN',0.9834,'door-anomaly-v1','1.2.3','algo-2.0.1','cfg-20261001',0.75,'sha256-mock-1008',NOW()-INTERVAL '30 minutes',NOW()-INTERVAL '29 minutes',231,'[{"category":"door_panel","confidence":0.9834,"bbox":"[120,80,460,420]","trackId":"track-0014"}]','["mock://evidence/1008/normal","mock://evidence/1008/abnormal"]','mock://task/AT-0014/raw',NULL,NULL,0,'TRACE-0014',false,'test',NOW(),'test',NOW()),
(3015,'AT-0015',NULL,1016,'C016',9004,'DOOR_ANOMALY_DETECT','DUAL_IMAGE','["mock://media/1016/normal","mock://media/1016/abnormal"]','mock://media/1016/normal','[120,80,460,420]','SUCCEEDED','UNCERTAIN','OTHER',0.0,'door-anomaly-v1','1.2.3','algo-2.0.1','cfg-20261001',0.75,'sha256-mock-1016',NOW()-INTERVAL '40 minutes',NOW()-INTERVAL '39 minutes',268,'[{"category":"door_panel","confidence":0.0,"bbox":"[120,80,460,420]","trackId":"track-0015"}]','["mock://evidence/1016/normal","mock://evidence/1016/abnormal"]','mock://task/AT-0015/raw',NULL,NULL,0,'TRACE-0015',false,'test',NOW(),'test',NOW()),
(3016,'AT-0016',NULL,1008,'C008',9004,'DOOR_ANOMALY_DETECT','DUAL_IMAGE','["mock://media/1008/normal","mock://media/1008/abnormal"]','mock://media/1008/normal','[120,80,460,420]','SUCCEEDED','NORMAL','ABNORMAL_MOVE',0.7744,'door-anomaly-v1','1.2.3','algo-2.0.1','cfg-20261001',0.75,'sha256-mock-1008',NOW()-INTERVAL '55 minutes',NOW()-INTERVAL '54 minutes',305,'[{"category":"door_panel","confidence":0.7744,"bbox":"[120,80,460,420]","trackId":"track-0016"}]','["mock://evidence/1008/normal","mock://evidence/1008/abnormal"]','mock://task/AT-0016/raw',NULL,NULL,0,'TRACE-0016',false,'test',NOW(),'test',NOW());

-- ========== 异动事件 ==========
INSERT INTO ai_anomaly_event (id,event_no,door_id,door_name,warehouse_id,warehouse_name,camera_id,camera_code,region_id,org_id,anomaly_type,anomaly_type_name,event_desc,occurred_at,recovered_at,algorithm_task_id,algorithm_status,algorithm_result,confidence,process_status,processed_by,processed_at,process_opinion,event_version,is_simulated,created_by,created_at,updated_by,updated_at) VALUES
(4001,'DA20260001',1006,'3号仓北门',9003,'三号粮库','C006','CAM-006','000000',NULL,'ILLEGAL_OPEN','非法开启','3号仓北门在非作业时段被开启，算法判定为异常开启。',NOW()-INTERVAL '3 minutes',NULL,3001,'SUCCEEDED','ANOMALY',0.9231,'PENDING',NULL,NULL,NULL,0,false,'test',NOW(),'test',NOW()),
(4002,'DA20260002',1003,'2号仓东门',9002,'二号粮库','C003','CAM-003','000000',NULL,'OTHER','其他或未知','2号仓东门开启，算法判定为正常。',NOW()-INTERVAL '8 minutes',NULL,3002,'SUCCEEDED','NORMAL',0.8812,'PENDING',NULL,NULL,NULL,0,false,'test',NOW(),'test',NOW()),
(4003,'DA20260003',1007,'4号仓东门',9004,'散粮库','C007','CAM-007','000000',NULL,'ABNORMAL_MOVE','异常移动','4号仓东门出现移动目标，算法无法判定。',NOW()-INTERVAL '12 minutes',NULL,3003,'SUCCEEDED','UNCERTAIN',NULL,'PROCESSING','zhangsan',NOW()-INTERVAL '6 minutes','已通知现场核实，等待反馈。',1,false,'test',NOW(),'test',NOW()),
(4004,'DA20260004',1009,'5号仓大门',9001,'一号粮库','C009','CAM-009','000000',NULL,'ILLEGAL_OPEN','非法开启','5号仓大门异常开启，算法执行失败，由人工核实确认。',NOW()-INTERVAL '20 minutes',NULL,3004,'FAILED',NULL,NULL,'VERIFIED_TRUE','lisi',NOW()-INTERVAL '15 minutes','现场确认仓门被非作业人员开启，已按流程上报。',2,false,'test',NOW(),'test',NOW()),
(4005,'DA20260005',1010,'6号仓东门',9002,'二号粮库','C010','CAM-010','000000',NULL,'ABNORMAL_MOVE','异常移动','6号仓东门检测到移动，算法推理超时。',NOW()-INTERVAL '25 minutes',NULL,3005,'TIMEOUT',NULL,NULL,'VERIFIED_FALSE','wangwu',NOW()-INTERVAL '18 minutes','经核实为正常作业期间人员走动，算法超时未出结论。',2,false,'test',NOW(),'test',NOW()),
(4006,'DA20260006',1014,'8号仓西门',9004,'散粮库','C014','CAM-014','000000',NULL,'DEVICE_OFFLINE','设备离线','8号仓西门摄像头离线。',NOW()-INTERVAL '45 minutes',NOW()-INTERVAL '25 minutes',3006,'SKIPPED',NULL,NULL,'CLOSED','zhaoliu',NOW()-INTERVAL '38 minutes','设备离线事件，无需算法识别，已通知运维。',1,false,'test',NOW(),'test',NOW()),
(4007,'DA20260007',1001,'1号仓东门',9001,'一号粮库','C001','CAM-001','000000',NULL,'ILLEGAL_OPEN','非法开启','模拟数据：1号仓东门异常开启。',NOW()-INTERVAL '35 minutes',NULL,3007,'MOCK','ANOMALY',0.7788,'PENDING',NULL,NULL,NULL,0,true,'test',NOW(),'test',NOW()),
(4008,'DA20260008',1011,'6号仓西门',9002,'二号粮库','C011','CAM-011','000000',NULL,'OTHER','其他或未知','6号仓西门产生候选异动，算法排队中。',NOW()-INTERVAL '2 minutes',NULL,3008,'QUEUED',NULL,NULL,'PENDING',NULL,NULL,NULL,0,false,'test',NOW(),'test',NOW()),
(4009,'DA20260009',1012,'7号仓北门',9003,'三号粮库','C012','CAM-012','000000',NULL,'OTHER','其他或未知','7号仓北门产生候选异动，算法识别中。',NOW()-INTERVAL '1 minutes',NULL,3009,'RUNNING',NULL,NULL,'PENDING',NULL,NULL,NULL,0,false,'test',NOW(),'test',NOW()),
(4010,'DA20260010',1002,'1号仓西门',9001,'一号粮库','C002','CAM-002','000000',NULL,'LONG_TIME_OPEN','长时间未关闭','1号仓西门开启超过30分钟未关闭。',NOW()-INTERVAL '180 minutes',NULL,3010,'SUCCEEDED','ANOMALY',0.9567,'VERIFIED_TRUE','sunli',NOW()-INTERVAL '150 minutes','现场确认仓门未及时关闭，已要求立即关闭。',2,false,'test',NOW(),'test',NOW()),
(4011,'DA20260011',1013,'8号仓东门',9004,'散粮库','C013','CAM-013','000000',NULL,'OTHER','其他或未知','8号仓东门状态未知，产生候选异动。',NOW()-INTERVAL '50 minutes',NULL,3011,'SUCCEEDED','UNCERTAIN',NULL,'PENDING',NULL,NULL,NULL,0,false,'test',NOW(),'test',NOW()),
(4012,'DA20260012',1005,'3号仓南门',9003,'三号粮库','C005','CAM-005','000000',NULL,'ILLEGAL_OPEN','非法开启','3号仓南门7天前异常开启记录。',NOW()-INTERVAL '10080 minutes',NULL,3012,'SUCCEEDED','ANOMALY',0.8123,'VERIFIED_TRUE','qianba',NOW()-INTERVAL '9960 minutes','7天前异常开启记录，用于验证默认时间范围过滤。',2,false,'test',NOW(),'test',NOW()),
(4013,'DA20260013',1004,'2号仓西门',9002,'二号粮库','C004','CAM-004','000000',NULL,'ABNORMAL_MOVE','异常移动','置信度偏低，需人工确认是否为真实异常。',NOW()-INTERVAL '15 minutes',NULL,3013,'SUCCEEDED','ANOMALY',0.5123,'PENDING',NULL,NULL,NULL,0,false,'test',NOW(),'test',NOW()),
(4014,'DA20260014',1008,'4号仓西门',9004,'散粮库','C008','CAM-008','000000',NULL,'ILLEGAL_OPEN','非法开启','4号仓西门高置信度异常开启，实为正常作业。',NOW()-INTERVAL '30 minutes',NULL,3014,'SUCCEEDED','ANOMALY',0.9834,'VERIFIED_FALSE','chenming',NOW()-INTERVAL '20 minutes','经核实为正常作业开启，算法高置信度误报。',2,false,'test',NOW(),'test',NOW()),
(4015,'DA20260015',1016,'散粮仓西门',9004,'散粮库','C016','CAM-016','000000',NULL,'OTHER','其他或未知','置信度为0，与缺失值区分显示。',NOW()-INTERVAL '40 minutes',NULL,3015,'SUCCEEDED','UNCERTAIN',0.0,'PENDING',NULL,NULL,NULL,0,false,'test',NOW(),'test',NOW()),
(4016,'DA20260016',1008,'4号仓西门',9004,'散粮库','C008','CAM-008','000000',NULL,'ABNORMAL_MOVE','异常移动','告警已恢复，算法判定正常。',NOW()-INTERVAL '55 minutes',NULL,3016,'SUCCEEDED','NORMAL',0.7744,'PENDING',NULL,NULL,NULL,0,false,'test',NOW(),'test',NOW());

-- ========== 事件证据 ==========
INSERT INTO ai_event_evidence (id,event_id,evidence_type,object_key,annotated_object_key,thumbnail_object_key,captured_at,camera_code,content_hash,sort_no,created_by,created_at) VALUES
(5001,4001,'NORMAL_REFERENCE','mock://evidence/1006/normal',NULL,'mock://thumb/1006/normal',NOW()-INTERVAL '4 minutes','CAM-006','sha256-normal-4001',1,'test','NOW()'),
(5002,4001,'ABNORMAL_KEY_FRAME','mock://evidence/1006/abnormal','mock://evidence/1006/abnormal-annotated','mock://thumb/1006/abnormal',NOW()-INTERVAL '3 minutes','CAM-006','sha256-abnormal-4001',2,'test','NOW()'),
(5003,4002,'NORMAL_REFERENCE','mock://evidence/1003/normal',NULL,'mock://thumb/1003/normal',NOW()-INTERVAL '9 minutes','CAM-003','sha256-normal-4002',1,'test','NOW()'),
(5004,4002,'ABNORMAL_KEY_FRAME','mock://evidence/1003/abnormal','mock://evidence/1003/abnormal-annotated','mock://thumb/1003/abnormal',NOW()-INTERVAL '8 minutes','CAM-003','sha256-abnormal-4002',2,'test','NOW()'),
(5005,4003,'NORMAL_REFERENCE','mock://evidence/1007/normal',NULL,'mock://thumb/1007/normal',NOW()-INTERVAL '13 minutes','CAM-007','sha256-normal-4003',1,'test','NOW()'),
(5006,4003,'ABNORMAL_KEY_FRAME','mock://evidence/1007/abnormal','mock://evidence/1007/abnormal-annotated','mock://thumb/1007/abnormal',NOW()-INTERVAL '12 minutes','CAM-007','sha256-abnormal-4003',2,'test','NOW()'),
(5007,4004,'NORMAL_REFERENCE','mock://evidence/1009/normal',NULL,'mock://thumb/1009/normal',NOW()-INTERVAL '21 minutes','CAM-009','sha256-normal-4004',1,'test','NOW()'),
(5008,4004,'ABNORMAL_KEY_FRAME','mock://evidence/1009/abnormal','mock://evidence/1009/abnormal-annotated','mock://thumb/1009/abnormal',NOW()-INTERVAL '20 minutes','CAM-009','sha256-abnormal-4004',2,'test','NOW()'),
(5009,4005,'NORMAL_REFERENCE','mock://evidence/1010/normal',NULL,'mock://thumb/1010/normal',NOW()-INTERVAL '26 minutes','CAM-010','sha256-normal-4005',1,'test','NOW()'),
(5010,4005,'ABNORMAL_KEY_FRAME','mock://evidence/1010/abnormal','mock://evidence/1010/abnormal-annotated','mock://thumb/1010/abnormal',NOW()-INTERVAL '25 minutes','CAM-010','sha256-abnormal-4005',2,'test','NOW()'),
(5011,4007,'NORMAL_REFERENCE','mock://evidence/1001/normal',NULL,'mock://thumb/1001/normal',NOW()-INTERVAL '36 minutes','CAM-001','sha256-normal-4007',1,'test','NOW()'),
(5012,4007,'ABNORMAL_KEY_FRAME','mock://evidence/1001/abnormal','mock://evidence/1001/abnormal-annotated','mock://thumb/1001/abnormal',NOW()-INTERVAL '35 minutes','CAM-001','sha256-abnormal-4007',2,'test','NOW()'),
(5013,4010,'NORMAL_REFERENCE','mock://evidence/1002/normal',NULL,'mock://thumb/1002/normal',NOW()-INTERVAL '181 minutes','CAM-002','sha256-normal-4010',1,'test','NOW()'),
(5014,4010,'ABNORMAL_KEY_FRAME','mock://evidence/1002/abnormal','mock://evidence/1002/abnormal-annotated','mock://thumb/1002/abnormal',NOW()-INTERVAL '180 minutes','CAM-002','sha256-abnormal-4010',2,'test','NOW()'),
(5015,4013,'NORMAL_REFERENCE','mock://evidence/1004/normal',NULL,'mock://thumb/1004/normal',NOW()-INTERVAL '16 minutes','CAM-004','sha256-normal-4013',1,'test','NOW()'),
(5016,4013,'ABNORMAL_KEY_FRAME','mock://evidence/1004/abnormal','mock://evidence/1004/abnormal-annotated','mock://thumb/1004/abnormal',NOW()-INTERVAL '15 minutes','CAM-004','sha256-abnormal-4013',2,'test','NOW()'),
(5017,4014,'NORMAL_REFERENCE','mock://evidence/1008/normal',NULL,'mock://thumb/1008/normal',NOW()-INTERVAL '31 minutes','CAM-008','sha256-normal-4014',1,'test','NOW()'),
(5018,4014,'ABNORMAL_KEY_FRAME','mock://evidence/1008/abnormal','mock://evidence/1008/abnormal-annotated','mock://thumb/1008/abnormal',NOW()-INTERVAL '30 minutes','CAM-008','sha256-abnormal-4014',2,'test','NOW()'),
(5019,4015,'NORMAL_REFERENCE','mock://evidence/1016/normal',NULL,'mock://thumb/1016/normal',NOW()-INTERVAL '41 minutes','CAM-016','sha256-normal-4015',1,'test','NOW()'),
(5020,4015,'ABNORMAL_KEY_FRAME','mock://evidence/1016/abnormal','mock://evidence/1016/abnormal-annotated','mock://thumb/1016/abnormal',NOW()-INTERVAL '40 minutes','CAM-016','sha256-abnormal-4015',2,'test','NOW()'),
(5021,4016,'NORMAL_REFERENCE','mock://evidence/1008/normal',NULL,'mock://thumb/1008/normal',NOW()-INTERVAL '56 minutes','CAM-008','sha256-normal-4016',1,'test','NOW()'),
(5022,4016,'ABNORMAL_KEY_FRAME','mock://evidence/1008/abnormal','mock://evidence/1008/abnormal-annotated','mock://thumb/1008/abnormal',NOW()-INTERVAL '55 minutes','CAM-008','sha256-abnormal-4016',2,'test','NOW()');

-- ========== 处理记录 ==========
INSERT INTO ai_event_process_record (id,event_id,event_no,status_before,status_after,process_result,process_opinion,processed_by,processed_at,source,created_at) VALUES
(6001,4003,'DA20260003','PENDING','PROCESSING','VERIFIED_TRUE','已通知现场核实，等待反馈。','zhangsan',NOW()-INTERVAL '14 minutes','WEB','NOW()'),
(6002,4004,'DA20260004','PENDING','PROCESSING','VERIFIED_TRUE','已通知现场核实，等待反馈。','lisi',NOW()-INTERVAL '23 minutes','WEB','NOW()'),
(6003,4004,'DA20260004','PROCESSING','VERIFIED_TRUE','VERIFIED_TRUE','现场确认仓门被非作业人员开启，已按流程上报。','lisi',NOW()-INTERVAL '15 minutes','WEB','NOW()'),
(6004,4005,'DA20260005','PENDING','PROCESSING','VERIFIED_TRUE','已通知现场核实，等待反馈。','wangwu',NOW()-INTERVAL '26 minutes','WEB','NOW()'),
(6005,4005,'DA20260005','PROCESSING','VERIFIED_FALSE','VERIFIED_FALSE','经核实为正常作业期间人员走动，算法超时未出结论。','wangwu',NOW()-INTERVAL '18 minutes','WEB','NOW()'),
(6006,4006,'DA20260006','PENDING','PROCESSING','VERIFIED_TRUE','已通知现场核实，等待反馈。','zhaoliu',NOW()-INTERVAL '46 minutes','WEB','NOW()'),
(6007,4010,'DA20260010','PENDING','PROCESSING','VERIFIED_TRUE','已通知现场核实，等待反馈。','sunli',NOW()-INTERVAL '158 minutes','WEB','NOW()'),
(6008,4010,'DA20260010','PROCESSING','VERIFIED_TRUE','VERIFIED_TRUE','现场确认仓门未及时关闭，已要求立即关闭。','sunli',NOW()-INTERVAL '150 minutes','WEB','NOW()'),
(6009,4012,'DA20260012','PENDING','PROCESSING','VERIFIED_TRUE','已通知现场核实，等待反馈。','qianba',NOW()-INTERVAL '9968 minutes','WEB','NOW()'),
(6010,4012,'DA20260012','PROCESSING','VERIFIED_TRUE','VERIFIED_TRUE','7天前异常开启记录，用于验证默认时间范围过滤。','qianba',NOW()-INTERVAL '9960 minutes','WEB','NOW()'),
(6011,4014,'DA20260014','PENDING','PROCESSING','VERIFIED_TRUE','已通知现场核实，等待反馈。','chenming',NOW()-INTERVAL '28 minutes','WEB','NOW()'),
(6012,4014,'DA20260014','PROCESSING','VERIFIED_FALSE','VERIFIED_FALSE','经核实为正常作业开启，算法高置信度误报。','chenming',NOW()-INTERVAL '20 minutes','WEB','NOW()');

-- 事件总数: 16
