package com.augmedix.utilities;

import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSonParser {

	public static JSONArray getJsonParsingData(Object[] jsonObject){
		String str = Arrays.deepToString(jsonObject);
		JSONArray aJsonArray = null;
		if (str.length() > 0) {
			str = getJsonObjectStrFromServerStr(str);
			System.out.println(str);
			JSONObject aJsonObject;
			try {
				aJsonObject = new JSONObject(str);
				 aJsonArray = aJsonObject
						.getJSONArray(Constants.ARRAY_NAME);
			/*	System.out.println("" + aJsonObject.has(Constants.ARRAY_NAME)
						+ " " + aJsonObject.has("dataa"));
				for (int i = 0; i < aJsonArray.length(); i++) {
					JSONObject aObject = aJsonArray
							.getJSONObject(i);
					System.out.println(aObject.getString(Constants.MSG));

				}*/
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {
			System.out.println("caused error");
		}
		return aJsonArray;
		
	}
	public static String getJsonObjectStrFromServerStr ( String jsonString )
	{
		if ( jsonString != null )
		{
			jsonString = jsonString.substring(1, jsonString.length() - 1);
		}
		
		return jsonString;
	}

}
