package org.arong.egdownloader.model;

import java.util.ArrayList;
import java.util.List;

import org.arong.egdownloader.spider.Spider;
import org.arong.egdownloader.spider.SpiderException;
import org.arong.egdownloader.spider.WebClient;
import org.arong.egdownloader.spider.WebClientException;

/**
 * Task分析引擎
 * @author 阿荣
 * @since 2014-05-25
 */
public final class ParseEngine {
	private static String url = "http://exhentai.org/g/701835/1ba4aa2531/";
	/**
	 * 步骤：
	 * 1、验证url的合法性。http://exhentai.org/g/446779/553f5c4086/
	 * 2、分析url构造hentai@home下载地址,通过hentai@home获取图片数量（size）、漫画名(name)、漫画查看地址集合
	 * 3、开启采集picture真实下载路径的任务。
	 * 4、根据pictures集合开始下载图片，存放到saveDir/name/目录下
	 * @throws SpiderException 
	 * @throws WebClientException 
	 */
	
	public static Task buildTask(String url, String saveDir, Setting setting) throws SpiderException, WebClientException{
		Task task = new Task(url, saveDir);

		String host = url.substring(0, url.indexOf(setting.getGidPrefix()));
		// 446779
		String gid = Spider.substring(url, setting.getGidPrefix()).substring(0,
				Spider.substring(url, setting.getGidPrefix()).indexOf("/"));
		// 553f5c4086
		String t = Spider.substring(url, gid + "/")
				.substring(0, Spider.substring(url, gid + "/").length())
				.replaceAll("/", "");
		//http://exhentai.org/hathdler.php?gid=446779&t=553f5c4086
		String hentaiHomeUrl = host + "/" + setting.getHentaiHome().getUri()
				+ "?" + setting.getHentaiHome().getFirstParameterName() + "="
				+ gid + "&" + setting.getHentaiHome().getSecondParameterName()
				+ "=" + t;
//		System.out.println(hentaiHomeUrl);
		//EHG-446779.hathdl文件内容
		String hentaiHomeSource = WebClient.postRequestWithCookie(hentaiHomeUrl, setting.getCookieInfo());
//		System.out.println(hentaiHomeSource);
		//数量
		String total_ = Spider.getTextFromSource(hentaiHomeSource, setting.getTotalPrefix(), "\n");
//		System.out.println(total);
		String name = Spider.getTextFromSource(hentaiHomeSource, setting.getNamePrefix(), "\n");
//		System.out.println(name);
		String fileList = Spider.getTextFromSource(hentaiHomeSource, setting.getFileListPrefix(), setting.getFileListSuffix());
//		System.out.println(fileList);
		
		int total = Integer.parseInt(total_.trim());
		//获取图片集合
		List<Picture> pictures = getPictures(fileList, total);
		setPicturesUrl(url, pictures, setting);
		
		task.setTotal(total);
		task.setName(name);
		task.setSaveDir(saveDir + "/" + name);
		task.setPictures(pictures);
		return task;
	}
	
	public static void main(String[] args) throws SpiderException, WebClientException {
		
		/*String gid = Spider.substring(url, "/g/").substring(0, Spider.substring(url, "/g/").indexOf("/"));
		System.out.println(gid);
		System.out.println(Spider.substring(url, gid + "/").substring(0, Spider.substring(url, gid + "/").length()).replaceAll("/", ""));
		System.out.println(url.substring(0, url.indexOf("/g/")));*/
		
		Setting setting = new Setting();
		Task task = buildTask(url, "", setting);
		for (Picture pic : task.getPictures()) {
			System.out.println(pic.getUrl() + ":" + pic.getNum());
		}
		
//		String str = Spider.getTextFromSource(WebClient.postRequestWithCookie(url + "?" + setting.getPageParam() + "=" + 0, setting.getCookieInfo()), url.substring(0, url.indexOf(setting.getGidPrefix())) + "/s/", "</html>");
//		System.out.println(str);
	}
	
	private static List<Picture> getPictures(String fileList, int total){
		if(fileList == null || "".equals(fileList.trim()) || total < 0){
			return null;
		}
		List<Picture> pictures = new ArrayList<Picture>();
		String[] rows = fileList.split("\n");
		String[] rowInfos;
		for(int i = 0; i < rows.length; i ++){
			//1 39fd09ca334866a6e80248da147ed1a39666b956-267169-1200-813-jpg Img_000.jpg
			rowInfos = rows[i].split(" ");
			Picture picture = new Picture();
			picture.setName(rowInfos[2]);
			pictures.add(picture);
		}
		return pictures;
	}
	
	private static void setPicturesUrl(String url, List<Picture> pictures, Setting setting) throws SpiderException, WebClientException{
		if(pictures != null){
			int total = pictures.size();
			//页数
			int page_num = total % setting.getPageCount() == 0 ? total / setting.getPageCount() : total / setting.getPageCount() + 1;
			String urlList;
			int count = setting.getPageCount();
			int picIndex;
			String showUrl;
			//http://exhentai.org/s/
			String showUrlPrefix = url.substring(0,url.indexOf(setting.getGidPrefix())) + setting.getShowPicPrefix();
			for(int i = 0; i < page_num; i ++){
				urlList = Spider.getTextFromSource(WebClient.postRequestWithCookie(url + "?" + setting.getPageParam() + "=" + i, setting.getCookieInfo()), showUrlPrefix, setting.getSourceSuffix());
				if(i == page_num - 1){
					count = total - (page_num - 1) * setting.getPageCount();
				}
				for(int j = 0; j < count; j ++){
					picIndex = i * setting.getPageCount() + j;
					showUrl = showUrlPrefix + urlList.substring(0, urlList.indexOf("\""));//Spider.getTextFromSource(urlList, showUrlPrefix , "\"");
					pictures.get(picIndex).setNum(genNum(total, picIndex));
					pictures.get(picIndex).setUrl(showUrl);
					if(urlList.indexOf(showUrlPrefix) != -1){
						urlList = Spider.substring(urlList, showUrlPrefix);
					}
				}
			}
		}
	}
	
	private static String genNum(int total, int index){
		int bit = 2;
		if(total <= 10){
			bit = 2;
		}else if(total <= 100){
			bit = 3;
		}else if(total <= 1000){
			bit = 4;
		}else if(total <= 10000){
			bit = 5;
		}else{
			bit = 6;
		}
		String num = "";
		int index_ = index + 1;
		for(int i = 1; i < bit - (index_ + "").length(); i++){
			num += "0";
		}
		return num + index_;
	}
}