CREATE EXTERNAL TABLE `wkp_text`(
  `parent_page_id` int, 
  `parent_revision_id` int, 
  `parent_header_id` int,
  `text` string,
  text_length int)
ROW FORMAT SERDE 
  'org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe' 
STORED AS INPUTFORMAT 
  'org.apache.hadoop.mapred.TextInputFormat' 
OUTPUTFORMAT 
  'org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat'
LOCATION
  's3://[bucket name]/wkp/wkp_text'
TBLPROPERTIES (
  'has_encrypted_data'='false')