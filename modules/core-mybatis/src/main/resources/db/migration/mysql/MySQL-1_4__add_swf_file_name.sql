ALTER TABLE fms_file_metadata ADD SWF_FILE_NAME VARCHAR(256) COMMENT '生成的swf文件对应的文件名';
ALTER TABLE fms_file_metadata ADD UNIQUE (`SWF_FILE_NAME`);