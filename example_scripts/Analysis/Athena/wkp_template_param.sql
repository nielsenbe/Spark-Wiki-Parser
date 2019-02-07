CREATE EXTERNAL TABLE `wkp_template_param`(
  `parent_page_id` int, 
  `parent_revision_id` int, 
  `parent_header_id` int, 
  `element_id` int, 
  `param_name` string,
  `param_value` string)
ROW FORMAT SERDE 
  'org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe' 
STORED AS INPUTFORMAT 
  'org.apache.hadoop.mapred.TextInputFormat' 
OUTPUTFORMAT 
  'org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat'
LOCATION
  's3://[bucket name]/wkp/wkp_template_param'
TBLPROPERTIES (
  'has_encrypted_data'='false')