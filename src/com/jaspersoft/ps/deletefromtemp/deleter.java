package com.jaspersoft.ps.deletefromtemp;

import java.sql.*;

import java.io.IOException;

import java.io.InputStream;

import java.io.StringWriter;

import java.util.ArrayList;

import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

public class deleter {

	// This is the first method that will simply call the delete on anything in the folder sent in.
	// So I will iterate into that folder and delete everything inside of it.
	// This is via the database
	public static String deleteviaDB(String driver, String connectionstring, String username, String password) throws SQLException, ClassNotFoundException
	{
	   Connection conn = null;
	   Statement stmt = null;

	   Class.forName(driver);

	  conn = DriverManager.getConnection(connectionstring, username, password);
	  
	  stmt = conn.createStatement();
	  stmt.execute("delete from jireportunitresource where resource_id in (select id from jifileresource where id in (select id from jiresource where parent_folder in (select id from jiresourcefolder where uri like '/temp/%')))");
	  stmt.execute("delete from jiadhocdataviewresource where resource_id in (select id from jifileresource where id in (select id from jiresource where parent_folder in (select id from jiresourcefolder where uri like '/temp/%')))");
	  stmt.execute("delete from jidatadefinerunit where id in (select id from jireportunit where mainreport in (select id from jifileresource where id in (select id from jiresource where parent_folder in (select id from jiresourcefolder where uri like '/temp/%'))))");
	  stmt.execute("delete from jireportunit where mainreport in (select id from jifileresource where id in (select id from jiresource where parent_folder in (select id from jiresourcefolder where uri like '/temp/%')))");
	  stmt.execute("delete from jifileresource where id in (select id from jiresource where parent_folder in (select id from jiresourcefolder where uri like '/temp/%'))");
	  stmt.execute("delete from jiadhocdataviewinputcontrol where input_control_id in (select id from  jiinputcontrol where id in (select id from jiresource where parent_folder in (select id from jiresourcefolder where uri like '/temp/%')))");
	  stmt.execute("delete from jiinputcontrolquerycolumn where input_control_id in (select id from  jiinputcontrol where id in (select id from jiresource where parent_folder in (select id from jiresourcefolder where uri like '/temp/%')))");
	  stmt.execute("delete from jiinputcontrol where id in (select id from jiresource where parent_folder in (select id from jiresourcefolder where uri like '/temp/%'))");
	  stmt.execute("delete from jiquery where id in (select id from jiresource where parent_folder in (select id from jiresourcefolder where uri like '/temp/%'))");
	  stmt.execute("delete from jiresource where parent_folder in (select id from jiresourcefolder where uri like '/temp/%')");
	  stmt.execute("delete from jiadhocdataview where id in (select id from jiresource where childrenfolder in (select id  from jiresourcefolder where uri like '/temp/%'))");
	  stmt.execute("delete from jiresource where childrenfolder in (select id  from jiresourcefolder where uri like '/temp/%')");
	  stmt.execute("delete from jiresourcefolder where uri like '/temp/%'");
	  
	  conn.close();
	  return "it worked!";
	}
	
	// This is the first method that will simply call the delete on anything in the folder sent in.
	// So I will iterate into that folder and delete everything inside of it.
	// This is via the database
	public static String deleteviarest() throws ClientProtocolException, IOException
	{
		String fullUrl = "http://localhost:8080/jasperserver-pro63/rest_v2/organizations/?j_username=superuser&j_password=superuser";
		
		List<String> orgFolderList = getOrgTempFolderList(fullUrl);

		List<String> tempFolderContentsList = new ArrayList<String>();

		for (String orgUri : orgFolderList) {
			fullUrl = "http://localhost:8080/jasperserver-pro63/rest_v2"
					+ "?folderUri=" + orgUri + "&j_username=superuser&j_password=superuser";
			tempFolderContentsList.addAll(executeHttpRequest(fullUrl, "resourceLookup", "uri"));
		}
		
		CloseableHttpClient httpClient = HttpClientBuilder.create().build();

		
		for (String tempResource : tempFolderContentsList) {
			
			fullUrl = "http://localhost:8080/jasperserver-pro63/rest_v2"
					+ tempResource + "?j_username=superuser&j_password=superuser";
			
			HttpDelete deleteRequest = new HttpDelete(fullUrl);
			HttpResponse response = httpClient.execute(deleteRequest);

		}
	

		return "Done with Rest delete.";
	}
	private static List<String> getOrgTempFolderList(String fullUrl)
			throws ClientProtocolException, IOException {
		List<String> strList = executeHttpRequest(fullUrl, "organization", "tenantFolderUri");
		List<String> updatedStrList = new ArrayList<String>();
		for (String thisString : strList) {
			updatedStrList.add(thisString + "/temp");
		}
		return updatedStrList;

	}



	/**
	 * @param fullUrl
	 * @param arrayNameToRetrieve
	 * @param valueFromArrayToRetrieve
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */

	public static List<String> executeHttpRequest(String fullUrl, String arrayNameToRetrieve,
			String valueFromArrayToRetrieve) throws ClientProtocolException, IOException {
		CloseableHttpClient httpClient = HttpClientBuilder.create().build();

		List<String> strList = new ArrayList<String>();
		try {
			HttpGet getRequest = new HttpGet(fullUrl);
			getRequest.setHeader("Accept", "application/json");
			// HttpPost postRequest = new HttpPost(fullUrl);
			HttpResponse response;
			response = httpClient.execute(getRequest);
			int statusCode = response.getStatusLine().getStatusCode();

			if (statusCode == 204) {
				return strList;
			}
			if (response.getStatusLine().getStatusCode() != 200) {
				System.out.println("Call to service did not work, returned status code: "
						+ response.getStatusLine().getStatusCode());
				System.exit(-1);
			}

			InputStream is = response.getEntity().getContent();

			String userInfoStr = convertInputStream2String(is);

			JSONObject json = new JSONObject();
			json = (JSONObject) JSONSerializer.toJSON(userInfoStr);

			JSONArray jOrgArray = json.getJSONArray(arrayNameToRetrieve);
			
			for (int i = 0; i < jOrgArray.length(); i++) {
				String strTenantFolderUri = jOrgArray.getJSONObject(i).getString(
						valueFromArrayToRetrieve);
				strList.add(strTenantFolderUri);
			}
		} catch (ClientProtocolException e) {
			System.out.println("ClientProtocolException inside getOrgTempFolderList");
			throw e;
		} catch (IOException e) {
			System.out.println("IOException inside getOrgTempFolderList");
			throw e;
		} finally {
			httpClient.close();
		}
		return strList;
	}
	protected static String convertInputStream2String(InputStream is) throws IOException {
		StringWriter writer = new StringWriter();
		IOUtils.copy(is, writer, "UTF-8");
		return writer.toString().trim();

	}

	
}
