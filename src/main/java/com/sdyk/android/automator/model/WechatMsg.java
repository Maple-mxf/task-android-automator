package com.sdyk.android.automator.model;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import one.rewind.db.DBName;
import one.rewind.db.DaoManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;

/**
 *
 * @author zhouxinhao33@gmail.com
 * @date
 */
@DBName(value = "android_auto")
@DatabaseTable(tableName = "wechat_msgs")
public class WechatMsg extends Model {

	public static enum Type {
		Image, Text, Time, File, Url
	}

	@DatabaseField(dataType = DataType.STRING, width = 32)
	public String device_id;

	@DatabaseField(dataType = DataType.STRING, width = 32)
	public String user_id;

	@DatabaseField(dataType = DataType.STRING, width = 32)
	public String user_name;

	@DatabaseField(dataType = DataType.STRING, width = 32)
	public String friend_name;

	//
	@DatabaseField(dataType = DataType.STRING, width = 1024)
	public String text;

	@DatabaseField(dataType = DataType.ENUM_STRING, width = 16)
	public Type text_type;

	@DatabaseField(dataType = DataType.BYTE_ARRAY)
	public byte[] content;

	public WechatMsg() {}

	public WechatMsg(String device_id, String user_id, String user_name) {
		this.device_id = device_id;
		this.user_id = user_id;
		this.user_name = user_name;
	}
}
