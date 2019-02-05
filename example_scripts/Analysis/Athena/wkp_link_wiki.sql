CREATE EXTERNAL TABLE `wkp_link_wiki`(
  `parent_page_id` int, 
  `parent_revision_id` int, 
  `parent_header_id` int, 
  `element_id` int, 
  `destination` string, 
  `destination_page_id` int, 
  `wiki_name_space` string, 
  `link_text` string, 
  `page_bookmark` string, 
  `page_exists` int)
ROW FORMAT SERDE 
  'org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe' 
STORED AS INPUTFORMAT 
  'org.apache.hadoop.mapred.TextInputFormat' 
OUTPUTFORMAT 
  'org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat'
LOCATION
  's3://[bucket name]/wkp/wkp_link_wiki'
TBLPROPERTIES (
  'has_encrypted_data'='false')