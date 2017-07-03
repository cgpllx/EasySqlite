package cc.easyandroid.easysqlite;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.ArrayList;

import cc.easyandroid.easysqlite.abs.DataAccesObject;
import cc.easyandroid.easysqlite.core.EasyDbObject;

/**
 * Class delegated charge of implementing CRUD methods for any object model.
 */
public class SQLiteDelegate<T extends EasyDbObject> implements DataAccesObject<T> {
    private final Gson GSON;//= new Gson();
    public static final String ID = "id";
    public static final String CREATEDTIME = "createdTime";
    public static final String GSONSTRING = "gson";
    protected final SQLiteOpenHelper helper;
    protected final String TABNAME;
    protected final Class<T> CLAZZ;


    public SQLiteDelegate(SQLiteOpenHelper helper, String TABNAME, Class<T> CLAZZ, Gson gson) {
        this.helper = helper;
        this.TABNAME = TABNAME;
        this.CLAZZ = CLAZZ;
        this.GSON = gson;
    }

    @Override
    public void insert(T dto) throws Exception {
        SQLiteDatabase db = getDb();
        ContentValues contentValues = new ContentValues();
        contentValues.put(ID, dto.buildKeyColumn());
        contentValues.put(CREATEDTIME, System.currentTimeMillis());
        contentValues.put(GSONSTRING, GSON.toJson(dto));
        long rowid = db.replace(TABNAME, null, contentValues);
        if (rowid == -1)
            throw new SQLiteException("Error inserting " + dto.getClass().toString());
    }

    @Override
    public void insertAll(ArrayList<T> arrayList) throws Exception {
        SQLiteDatabase db = getDb();
        try {
            db.beginTransaction();
            for (int i = 0; i < arrayList.size(); i++) {
                T dto = arrayList.get(i);
                insert(dto);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public T findById(String id, Type type) throws Exception {
        SQLiteDatabase db = getDb();
        String selection = ID + "=?";
        String[] selectionArgs = {id};
        String orderBy = CREATEDTIME + " " + "DESC";//
        Cursor cursor = db.query(TABNAME, null, selection, selectionArgs, null, null, orderBy);
        T easyDbObject = null;
        try {
            if (cursor.moveToFirst()) {
                String gson = cursor.getString(cursor.getColumnIndex("gson"));
                easyDbObject = GSON.fromJson(gson, type);
            }
        } catch (Exception e) {
            throw new SQLiteException("Error findAllFromTabName " + TABNAME);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return easyDbObject;
    }

    @Override
    public T findById(String id) throws Exception {
        return findById(id, CLAZZ);
    }


    @Override
    public synchronized boolean delete(String id) throws Exception {
        SQLiteDatabase db = getDb();
        String whereClause = ID + "=?";
        String[] whereArgs = {id};
        int confirm = db.delete(TABNAME, whereClause, whereArgs);
        return confirm != 0;
    }

    @Override
    public synchronized boolean deleteAll() throws Exception {
        SQLiteDatabase db = getDb();
        int confirm = db.delete(TABNAME, null, null);
        return confirm != 0;
    }

    @Override
    public ArrayList<T> findAllFromTabName(String orderBy, Type type) throws Exception {
        Cursor cursor = findAllCursor(orderBy);
        ArrayList<T> list = new ArrayList<>();
        try {
            if (cursor.moveToFirst()) {
                do {
                    String gson = cursor.getString(cursor.getColumnIndex("gson"));
                    T t = GSON.fromJson(gson, type);
                    list.add(t);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            throw new SQLiteException("Error findAllFromTabName " + TABNAME);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return list;
    }

    /**
     * @param order eg "_id" DESC  时间正序还是倒序
     * @return Cursor
     */
    @Override
    public Cursor findAllCursor(String order) {
        SQLiteDatabase db = getDb();
        String orderBy = CREATEDTIME + " " + order;
        return db.query(TABNAME, null, null, null, null, null, orderBy);
    }

    /**
     * @param orderBy eg "_id" DESC  时间正序还是倒序
     * @return ArrayList
     */
    @Override
    public ArrayList<T> findAllFromTabName(String orderBy) throws Exception {
        return findAllFromTabName(orderBy, CLAZZ);
    }

    private SQLiteDatabase mSQLiteDatabase;

    private SQLiteDatabase getDb() {
        if (mSQLiteDatabase == null) {
            mSQLiteDatabase = helper.getWritableDatabase();
        }
        return mSQLiteDatabase;
    }
}