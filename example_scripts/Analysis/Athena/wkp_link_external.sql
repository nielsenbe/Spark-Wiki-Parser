CREATE EXTERNAL TABLE `wkp_link_external`(
  `parent_page_id` int, 
  `parent_revision_id` int, 
  `parent_header_id` int, 
  `element_id` int, 
  `destination` string, 
  `link_text` string,
  `domain` string,
  `page_bookmark` string)
ROW FORMAT SERDE 
  'org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe' 
STORED AS INPUTFORMAT 
  'org.apache.hadoop.mapred.TextInputFormat' 
OUTPUTFORMAT 
  'org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat'
LOCATION
  's3://[bucket name]/wkp/wkp_link_external'
TBLPROPERTIES (
  'has_encrypted_data'='false')