CREATE EXTERNAL TABLE `wkp_redirect`(
  `target_page_id` int, 
  `redirect_page_id` int, 
  `redirect_title` string)
ROW FORMAT SERDE 
  'org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe' 
STORED AS INPUTFORMAT 
  'org.apache.hadoop.mapred.TextInputFormat' 
OUTPUTFORMAT 
  'org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat'
LOCATION
  's3://[bucket name]/wkp/wkp_redirect'
TBLPROPERTIES (
  'has_encrypted_data'='false')