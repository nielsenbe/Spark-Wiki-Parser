CREATE EXTERNAL TABLE `wkp_template`(
  `parent_page_id` int, 
  `parent_revision_id` int, 
  `parent_header_id` int, 
  `element_id` int, 
  `template_type` string,
  `is_info_box` boolean)
ROW FORMAT SERDE 
  'org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe' 
STORED AS INPUTFORMAT 
  'org.apache.hadoop.mapred.TextInputFormat' 
OUTPUTFORMAT 
  'org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat'
LOCATION
  's3://[bucket name]/wkp/wkp_template'
TBLPROPERTIES (
  'has_encrypted_data'='false')