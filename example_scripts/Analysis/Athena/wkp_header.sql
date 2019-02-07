CREATE EXTERNAL TABLE `wkp_header`(
parent_page_id int,
parent_revision_id int,
header_id int,
title string,
header_level int,
is_ancillary int,
text_length int,
wiki_link_count int,
template_count int,
cleanup_template_count int)
ROW FORMAT SERDE 
  'org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe' 
STORED AS INPUTFORMAT 
  'org.apache.hadoop.mapred.TextInputFormat' 
OUTPUTFORMAT 
  'org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat'
LOCATION
  's3://[bucket name]/wkp/wkp_header'
TBLPROPERTIES (
  'has_encrypted_data'='false')