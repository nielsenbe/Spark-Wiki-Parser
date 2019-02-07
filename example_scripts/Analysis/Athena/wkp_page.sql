CREATE EXTERNAL TABLE `wkp_page`(
  `id` int, 
  `title` string, 
  `entity` string, 
  `name_space_num` int, 
  `name_space_text` string, 
  `page_type` string, 
  `revision_id` bigint, 
  `revision_date` bigint, 
  `text_length` int, 
  `wiki_link_count` int, 
  `incoming_wiki_link_count` int, 
  `template_count` int, 
  `cleanup_template_count` int)
ROW FORMAT SERDE 
  'org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe' 
STORED AS INPUTFORMAT 
  'org.apache.hadoop.mapred.TextInputFormat' 
OUTPUTFORMAT 
  'org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat'
LOCATION
  's3://[bucket name]/wkp/wkp_page'
TBLPROPERTIES (
  'has_encrypted_data'='false')