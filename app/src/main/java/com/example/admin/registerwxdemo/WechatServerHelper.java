package com.example.admin.registerwxdemo;

import android.util.Log;

import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.Callback;
import com.zhy.http.okhttp.request.RequestCall;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.Response;

/**
 * ????????????sdasdasdasdasd
 */
public class WechatServerHelper {

	static WechatServerHelper mWechatServerHelper = null;
	public static String BASE_URL = "http://192.168.0.213:8000/";

	/**??????*/
	final public static int CHANGE_PASS = 0x1;
	final public static int CHANGE_NICKNAME = 0x2;
	final public static int CHANGE_WXID = 0x3;
	final public static int CHANGE_BINDPHONE = 0x4;

	/**??????????optType*/
	final public static int ADD_PEOPLE = 0x1;
	final public static int PASS_PEOPLE = 0x2;
	final public static int INITIATIVE_PUSHCARD_NUM = 0x3;// ???????????????
	final public static int PASSIVE_PUSHCARD_NUM = 0x4;// ????????????????

	/**??????????*/
	final public static int OPTNOTIFY_TYPE_ADD_CARDWX = 1;
	final public static int OPTNOTIFY_TYPE_CARDWX_EXIST = 2;
	final public static int OPTNOTIFY_RES_SUCCESS = 1;
	final public static int OPTNOTIFY_RES_FAILED = 2;

	public static WechatServerHelper getInstance() {
		if (mWechatServerHelper == null)
			mWechatServerHelper = new WechatServerHelper();
		return mWechatServerHelper;
	}

	public class PostCallback extends Callback<String> {
		private String res;
		private AtomicEvent atomicEvent;

		public PostCallback() {
			res = null;
			atomicEvent = new AtomicEvent();
		}

		public String getResponse() {
			return res;
		}

		public boolean waitWithTimeout(long mills) throws InterruptedException {
			return atomicEvent.Wait(mills);
		}

		@Override
		public String parseNetworkResponse(Response arg0, int arg1) throws Exception {
			Log.i("post", "parseNetworkResponse, arg0 = " + (arg0 == null ? "null" : arg0.toString()));
			if (arg0 != null) {
				String string = arg0.body().string();
				if (string != null) {
					byte[] decrypt = AESUtils.decrypt(AESUtils.hexStringToBytes(string));
					res = new String(decrypt, "utf-8");
					if(res != null) {
						atomicEvent.getAndIncrement();
					}
					return res;
				}
			}
			return null;
		}

		@Override
		public void onResponse(String arg0, int arg1) {
			Log.i("post", "onResponse.arg0 = " + arg0);
			atomicEvent.getAndIncrement();
		}

		@Override
		public void onError(Call arg0, Exception arg1, int arg2) {
			Log.i("post", "onError, error = " + (arg1 == null ? "null" : arg1.getMessage()));
			atomicEvent.getAndIncrement();
		}
	}

	public boolean isEncryptStringCanDecrypt(String hexEncryptS) {
		try {
			AESUtils.decrypt(AESUtils.hexStringToBytes(hexEncryptS));
			return true;
		} catch (Exception e) {
		}
		return false;
	}

	public String postJSon(String url, JSONObject postData) {
		try {
			PostCallback postCallback = null; // ??????
			RequestCall requestCall = null; // http????
			byte[] encrypt = AESUtils.encrypt(postData.toString()); // ?????
			String encrptHexS = AESUtils.bytesToHexString(encrypt); // ????HEX
			Log.i("post", "isEncryptStringCanDecrypt = " + isEncryptStringCanDecrypt(encrptHexS));// ?????????log???encrptHexS

			for (int i = 0; i < 3; i++) {
				long ts = System.currentTimeMillis(); // TAG
				postCallback = new PostCallback() {};
				requestCall = OkHttpUtils
						.postString()
						.url(url)// ?post?url
						.content(encrptHexS)// ?post???
						.mediaType(MediaType.parse("application/json; charset=utf-8")) // post????json??
						.tag(ts) // ??tag
						.build();

				try {
					requestCall.connTimeOut(60000).writeTimeOut(60000).readTimeOut(60000).execute(postCallback); // ?????????http??
					Log.i("post", "waitWithTimeout");
					if (postCallback.waitWithTimeout(60 * 1000) == false) { // wait
																			// 60s??????onReponse??onError??????
						OkHttpUtils.getInstance().cancelTag(ts);
						requestCall = null;
						postCallback = null;
						Thread.sleep(10000); // ??10s????????
						continue;
					}

					String res = postCallback.getResponse(); // ?????
					if (res != null) {
						return res;
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					OkHttpUtils.getInstance().cancelTag(ts);
					requestCall = null;
					postCallback = null;
				}
				Thread.sleep(10000); // ??10s????????
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * ???post???????post???true or false
	 */
	public  boolean postDataToServerAndRetResult(String url, JSONObject postData) {
		Log.i("post", "url = " + url);
		Log.i("post", "postData = " + postData.toString());
		try {
			String response = postJSon(url, postData);
			Log.i("post", "response = " + response);
			if (response != null) {
				JSONObject resJSON = new JSONObject(response);
				if(resJSON != null && resJSON.has("result") == true) {
					Log.i("post", "resJSON = " + resJSON.toString());
					int result = resJSON.getInt("result");
					if (result == 1)
						return true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * ???post????????????
	 */
	public JSONObject postDataToServerAndGetRes(String url, JSONObject postData) {
		Log.i("post", "url = " + url);
		Log.i("post", "postData = " + postData.toString());
		JSONObject resJSON = null;
		try {
			String response = postJSon(url, postData);
			Log.i("post", "response = " + response);
			if (response != null) {
				resJSON = new JSONObject(response);
			}
		} catch (Exception e) {
			Log.i("post", "error = " + e.getMessage());
			e.printStackTrace();
		}
		return resJSON;
	}

	/**
	 * ?????Job
	 */
	public JSONObject getJob(int userid, String url) throws JSONException {
		try {
			JSONObject postData = new JSONObject();
			postData.put("userid", userid);
			return postDataToServerAndGetRes(url, postData);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * ??????????
	 */
	public JSONObject getWx(int chn_id, int userStatus, String logonMechine) throws IOException, JSONException {
		JSONObject postData = new JSONObject();
		postData.put("chn_id", chn_id);
		postData.put("logonMechine", logonMechine);
		postData.put("userStatus", userStatus);
		String url = BASE_URL + "wechat/getwx";
		return postDataToServerAndGetRes(url, postData);
	}

	/**
	 * ??????????????????id?????
	 */
	public boolean changeWx(int userid, String extraInfo, int type) throws IOException, JSONException {
		JSONObject postData = new JSONObject();
		postData.put("userid", userid);
		switch (type) {
		case CHANGE_PASS:
			postData.put("newPass", extraInfo);
			break;
		case CHANGE_NICKNAME:
			postData.put("newNickName", extraInfo);
			break;
		case CHANGE_WXID:
			postData.put("newWxId", extraInfo);
			break;
		case CHANGE_BINDPHONE:
			postData.put("newPhone", extraInfo);
			break;
		default:
			break;
		}
		String url = BASE_URL + "wechat/changewx";
		return postDataToServerAndRetResult(url, postData);
	}

	/**
	 * ?????????????????????
	 */
	public JSONObject userInfoForOld(int userid, String username, String password, String logonMechine, int wxLogonType, String appenvInfo,
			int chn_id, int status, String softVersion, String wxVersion, String wxId, JSONObject wx62data) throws JSONException, IOException {
		JSONObject postData = new JSONObject();
		if(userid != -1)
			postData.put("userid", userid);
		postData.put("user", username);
		if(password != null)
			postData.put("pass", password);
		postData.put("logonMechine", logonMechine);
		postData.put("wxLogonType", wxLogonType);
		postData.put("info", appenvInfo);
		if(chn_id != -1)
			postData.put("chn_id", chn_id);
		postData.put("soft_version", softVersion);
		postData.put("v_version", wxVersion);
		postData.put("status", status);
		if(wxId != null)
			postData.put("wxid", wxId);
		if(wx62data != null)
			postData.put("wx62data", wx62data);
		String url = BASE_URL + "wechat/userInfoForOld";
		return postDataToServerAndGetRes(url, postData);
	}

	/**
	 * ?????????????????????????????
	 */
	public boolean workStatistics(int userid, int optType, int cnt, JSONObject peopleJson, String phone, int taskChnId) throws IOException, JSONException {
		JSONObject postData = new JSONObject();
		postData.put("userid", userid);
		postData.put("optType", optType);
		postData.put("cnt", cnt);
		if (optType == INITIATIVE_PUSHCARD_NUM || optType == PASSIVE_PUSHCARD_NUM) {
			if (peopleJson != null)
				postData.put("param", peopleJson.toString());
		}
		if(phone != null)
			postData.put("phone", phone);
		if(taskChnId != -1)
			postData.put("taskchn_id", taskChnId);
		String url = BASE_URL + "wechat/work_statistics";
		return postDataToServerAndRetResult(url, postData);
	}

	/**
	 * ?????????????{info : [{nickname1:phone1}, {nickname2:phone2}, {nickname3:phone3}...]}
	 */
	public boolean uploadAddedUserInfo(JSONObject userInfo) {
		String url = BASE_URL + "material/remarks_friend";
		return postDataToServerAndRetResult(url, userInfo);
	}

	/**
	 * ???
	 */
	public boolean keepLive(String user, String logonMechine, int status, String info, String softVersion, String wxVersion) throws IOException, JSONException {
		JSONObject postData = new JSONObject();
		postData.put("user", user);
		postData.put("logonMechine", logonMechine);
		postData.put("status", status);
		postData.put("v_version", wxVersion);
		postData.put("soft_version", softVersion);
		if (info != null)
			postData.put("info", info);
		String url = BASE_URL + "wechat/userInfo";
		return postDataToServerAndRetResult(url, postData);
	}

	/**
	 * ??????????????????AddFriends???Job
	 */
	public boolean phoneStatusChange(int userid, int taskchn_id, String phone, String rawnickname, int status) throws IOException, JSONException {
		JSONObject postData = new JSONObject();
		postData.put("userid", userid);
		postData.put("taskchn_id", taskchn_id);
		postData.put("phone", phone);
		if(rawnickname != null)
			postData.put("rawnickname", rawnickname);
		postData.put("status", status);
		String url = BASE_URL + "material/add_friend";
		return postDataToServerAndRetResult(url, postData);
	}

	/**
	 * ??????????????????AddFriends???Job
	 */
	public boolean phoneStatusChangeBat(int userid, int taskchn_id, JSONArray userInfos, int status) throws IOException, JSONException {
		JSONObject postData = new JSONObject();
		postData.put("userid", userid);
		postData.put("taskchn_id", taskchn_id);
		postData.put("phones", userInfos);
		postData.put("status", status);
		String url = BASE_URL + "material/add_friend_bat";
		return postDataToServerAndRetResult(url, postData);
	}

	/**
	 * ??????????????????	//new add in v2.2
	 *	POST http://www.xxx.com/material/friend_ok	(??) { phone:"xxxx", wxid:"xxxxxxxxxxxxxx"(???,??wxid), sex:xx, status:3 }
	 */
	public boolean friendOk(String phone, int taskChnId, String wxid, int sex, int status) throws IOException, JSONException {
		JSONObject postData = new JSONObject();
		postData.put("phone", phone);
		if(taskChnId != -1)
			postData.put("taskchn_id", taskChnId);
		postData.put("wxid", wxid);
		postData.put("sex", sex);
		postData.put("status", status);
		String url = BASE_URL + "material/friend_ok";
		return postDataToServerAndRetResult(url, postData);
	}

	/**
	 * ?????
	 */
	public JSONObject requestContacts(int userid, int taskchn_id) throws IOException, JSONException {
		JSONObject postData = new JSONObject();
		postData.put("userid", userid);
		postData.put("taskchn_id", taskchn_id);
		String url = BASE_URL + "material/addrlist";
		return postDataToServerAndGetRes(url, postData);
	}

	/**
	 * ??????????RestoreDataJob
	 */
	public JSONObject getbackwxinfo(String logonMechine) throws JSONException {
		JSONObject postData = new JSONObject();
		postData.put("logonMechine", logonMechine);
		String url = BASE_URL + "wechat/getbackwxinfo";
		return postDataToServerAndGetRes(url, postData);
	}

	/**
	 * ??????
	 */
	public boolean laqunStatistics(int userid, int qunchn_id, String qunname, JSONArray wxnickname, String extraMsg) throws JSONException {
		JSONObject postData = new JSONObject();
		postData.put("userid", userid);
		postData.put("qunchn_id", qunchn_id);
		postData.put("qunname", qunname);
		postData.put("wxnickname", wxnickname);
		postData.put("extraMsg", extraMsg);

		Log.i("laqun", "postData = " + postData.toString());
		String response = postJSon(BASE_URL + "wechat/laqun_statistics", postData);
		Log.i("laqun", "response = " + response);
		if (response != null) {
			try {
				JSONObject json = new JSONObject(response);
				if(json.has("okcnt") == true && json.has("failcnt") == true) {
					return true;
				} else {
					return false;
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	/**
	 * ??????
	 */
	public boolean qunPeopleStatistics(int userid, int qunChnId, String qunName, int count, String extraMsg) throws JSONException {
		JSONObject postData = new JSONObject();
		postData.put("userid", userid);
		postData.put("qunchn_id", qunChnId);
		postData.put("qunname", qunName);
		postData.put("count", count);
		postData.put("extraMsg", extraMsg);
		String url = BASE_URL + "wechat/qunpeople_statistics";
		return postDataToServerAndRetResult(url, postData);
	}

	/**
	 * ????????????
	 */
	public boolean optNotify(int userid, int type, JSONObject params, String extraMsg, int result) throws JSONException {
		JSONObject postData = new JSONObject();
		postData.put("userid", userid);
		postData.put("type", type);
		postData.put("params", params);
		postData.put("extraMsg", extraMsg);
		postData.put("result", result);
		String url = BASE_URL + "wechat/optnotify";
		return postDataToServerAndRetResult(url, postData);
	}

	/**
	 * ???????,??????,?????? ??????????
	 * POST http://www.xxx.com/material/phone_blacklist_bat	(v1.1??) (??????????)
	 * {userid:xxxx, taskchn_id:xxx, phones:[{phone1:judgeType1},{phone2:judgeType2},{phone3:judgeType3}...]}
	 */
	public boolean uploadPhoneBlacklist(int userid, int taskChnId, JSONArray phones) throws JSONException {
		JSONObject postData = new JSONObject();
		postData.put("userid", userid);
		postData.put("taskChnId", taskChnId);
		postData.put("phones", phones);
		String url = BASE_URL + "material/phone_blacklist_bat";
		return postDataToServerAndRetResult(url, postData);
	}

	public  boolean uploadPhoneBlacklistBySpaceid(int userid, int spaceid, JSONArray phones) throws JSONException {
		JSONObject postData = new JSONObject();
		postData.put("userid", userid);
		postData.put("spaceid", spaceid);  // 1
		postData.put("phones", phones);
		String url = BASE_URL + "material/phone_blacklist_bat";
		return postDataToServerAndRetResult(url, postData);
	}

	/**
	 * ?????phones?????????wxid
	 */
	public JSONObject addrlistForTranslateWxid(int userid, int spaceid, int num , int bForTranslate) throws JSONException {
		JSONObject postData = new JSONObject();
		postData.put("userid", userid);
		postData.put("spaceid", spaceid);
		postData.put("bForTranslate", bForTranslate);
		postData.put("num", num);
		String url = BASE_URL + "material/addrlist_forTranslateWxid";
		return postDataToServerAndGetRes(url, postData);
	}

	/**
	 * ??wxid
	 */
	public boolean translateWxidOk(int userid, int spaceid, JSONObject infos) throws JSONException {
		JSONObject postData = new JSONObject();
		postData.put("userid", userid);
		postData.put("spaceid", spaceid);
		postData.put("phones", infos);
		String url = BASE_URL + "material/translateWxid_ok";
		return postDataToServerAndRetResult(url, postData);
	}



	/**
	 * ??wxid???wxid??
	 */
	public JSONObject requestWxidlist(int userid, int taskChnId, int num) throws JSONException {
		JSONObject postData = new JSONObject();
		postData.put("userid", userid);
		postData.put("taskchn_id", taskChnId);
		postData.put("num", num);
		String url = BASE_URL + "material/wxidlist";
		return postDataToServerAndGetRes(url, postData);
	}


	/**
	 * ???????????
	 */
	public JSONObject phoneExistenceBat(int userid, int spaceid, JSONArray phones) throws JSONException {
		JSONObject postData = new JSONObject();
		postData.put("userid", userid);
		postData.put("spaceid", spaceid);
		postData.put("phones", phones);
		String url = BASE_URL + "material/phone_existence_bat";
		return postDataToServerAndGetRes(url, postData);
	}
}
