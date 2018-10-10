package one.rewind.android.automator.model;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import one.rewind.db.DaoManager;
import one.rewind.json.JSON;
import one.rewind.json.JSONable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;

/**
 *
 */
public abstract class Model implements JSONable<Model> {

	private static final Logger logger = LogManager.getLogger(Model.class.getName());

	@DatabaseField(dataType = DataType.INTEGER, index = true, generatedId = true)
	public int id;

	@DatabaseField(dataType = DataType.DATE)
	public Date insert_time = new Date();

	@DatabaseField(dataType = DataType.DATE)
	public Date update_time = new Date();

	/**
	 *
	 * @return
	 * @throws Exception
	 */
	public boolean insert() throws Exception {
		Dao dao = DaoManager.getDao(this.getClass());
		if (dao.create(this) == 1) {
			return true;
		}
		return false;
	}

	/**
	 *
	 * @return
	 * @throws Exception
	 */
	public boolean update() throws Exception {
		Dao dao = DaoManager.getDao(this.getClass());
		if (dao.update(this) == 1) {
			return true;
		}
		return false;
	}

	/**
	 *
	 * @param clazz
	 * @param id
	 * @return
	 * @throws Exception
	 */
	public static Model getById(Class clazz, String id) throws Exception {

		Dao dao = DaoManager.getDao(clazz);

		return (Model) dao.queryForId(id);
	}

	@Override
	public String toJSON() {
		return JSON.toJson(this);
	}
}
