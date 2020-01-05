import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.HttpURLConnection;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.BufferedOutputStream;

public class ChesenyMicroBlogDownloader {

  private static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.88 Safari/537.36";

  public String uidToContainerId(String uid) {
    if (uid == null)
      throw new IllegalArgumentException("uid is null");
    return "107603".concat(uid);
  }

  public String nicknameToContainerId(String nickname) {
    String url = "http://m.weibo.com/n/" + nickname;
    http("GET", url);
    return null;
  }

  public String usernameToContainerId(String username) {
    String url = "https://weibo.cn/" + username;
    http("GET", url);
    return null;
  }

  public void start(final ChesenyMicroBlogDownloader chesenyMicroBlogDownloader, String type, String name, String filePath) {
    String containerId = null;
    if (type.equals("nickname")) {
      containerId = nicknameToContainerId(name);
    } else if (type.equals("username")) {
      containerId = usernameToContainerId(name);
    } else if (type.equals("uid")) {
      containerId = uidToContainerId(name);
    }
    if (containerId == null) {
      System.out.println("cannot find containerId");
      return;
    }
    String jsonPath = filePath + "/json/";
    if (!filePath.endsWith("/") && !filePath.endsWith("\\")) {
      if(filePath.contains("/")) {
        filePath = filePath + "/" + containerId.substring(6) + "/";
        jsonPath = filePath + "/" + "json/";
      } else {
        filePath = filePath + "\\" +  containerId.substring(6) + "\\";
        jsonPath = filePath + "\\" + "json\\";
      }
    }
    if (!new File(filePath).exists()) {
      try {
        new File(filePath).mkdirs();
      } catch (Exception e) {
      }
    }
    if (!new File(jsonPath).exists()) {
      try {
        new File(jsonPath).mkdirs();
      } catch (Exception e) {
      }
    }
    List<String> imgUrls = getAllImgURL(containerId, jsonPath);
    System.out.println("the task will be starting!");
    System.out.println("totals: " + imgUrls.size());
    final CountDownLatch downLatch = new CountDownLatch(imgUrls.size());
    ExecutorService executor = Executors.newFixedThreadPool(4);
    final String destPath = filePath;
    for (int i = 0; i < imgUrls.size(); i++) {
      final String imageUrl = imgUrls.get(i);
      final int imageIndex = i;
      executor.submit(() -> {
        try {
          System.out.println("download: " + (imageIndex + 1));
          byte[] imgBytes = chesenyMicroBlogDownloader.http("GET", imageUrl);
          chesenyMicroBlogDownloader.byte2File(imgBytes, destPath, imageIndex + 1 + chesenyMicroBlogDownloader.getSuffix(imageUrl));
        }catch (Exception e) {
        }finally {
          try {
            downLatch.countDown();
          } catch (Exception e) {
          }
        }
      });
    }
    try {
      downLatch.await();
    } catch (Exception e) {
    }
    System.out.println("it is finished: " + filePath);
    executor.shutdown();
  }

  private String getSuffix(String url) {
    if (!url.substring(url.lastIndexOf("/")).contains(".")) {
      return ".jpg";
    }
    try {
      return url.substring(url.lastIndexOf("."));
    } catch(Exception e) {
    }
    return ".jpg";
  }

  public static void byte2File(byte[] buf, String path, String fileName) {
    BufferedOutputStream bos = null;
    FileOutputStream fos = null;
    File file = null;
    File pathF = new File(path);
    if (!pathF.exists()) {
      pathF.mkdirs();
    }
    try {
      file = new File(pathF, fileName);
      fos = new FileOutputStream(file);
      bos = new BufferedOutputStream(fos);
      bos.write(buf);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (bos != null) {
        try {
          bos.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      if (fos != null) {
        try {
          fos.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  public byte[] http(String method, String url) {
    HttpURLConnection httpURLConnection = null;
    InputStream inputStream = null;
    try {
      httpURLConnection = (HttpURLConnection) new URL(url).openConnection();
      httpURLConnection.setRequestMethod(method);
      httpURLConnection.setDoInput(true);
      httpURLConnection.setDoOutput​(true);
      httpURLConnection.setConnectTimeout(1000 * 60);
      httpURLConnection.setReadTimeout(1000 * 60);
      httpURLConnection.setRequestProperty("User-Agent", USER_AGENT);
      if (httpURLConnection.getResponseCode() == 302) {
        Map<String, List<String>> map = httpURLConnection.getHeaderFields();
        System.out.println(map);
      }
      if (httpURLConnection.getResponseCode() == 200) {
        inputStream = httpURLConnection.getInputStream();
        return inputStream.readAllBytes();
      }
    } catch (MalformedURLException e) {
    } catch (ProtocolException e) {
    } catch (IOException e) {
    } finally {
      if (httpURLConnection != null) {
        httpURLConnection.disconnect();
      }
      try {
        if (inputStream != null) {
          inputStream.close();
        }
      } catch (Exception e) {
      }
    }
    return null;
  }

  public List<String> getAllImgURL(String containerid, String jsonPath) {
    List<String> urls = new ArrayList<String>();
    int i = 1;
    while (getImgURL(containerid, i, urls, jsonPath) > 0) {
      System.out.println("it is analyzing: " + i);
      i++; if (i > 1) break;
      try {
        Thread.sleep(2000);
      } catch (Exception e) {
      }
    }
    return urls;
  }

  private int getImgURL(String containerid, int page, List<String> urls, String jsonPath) {
    String url = "https://m.weibo.cn/api/container/getIndex?count=25&page=" + page + "&containerid=" + containerid;
    String content = null;
    try {
      content = new String(http("GET", url), "utf-8");
    } catch (Exception e) {
    }
    JsonObject root = new JsonParser().parse(content).getAsJsonObject();
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    OutputStream outputStream = null;
    try {
      File file = new File(jsonPath + page + ".json");
      outputStream = new FileOutputStream(file);
      outputStream.write(gson.toJson(root).getBytes());
      outputStream.flush();
      outputStream.close();
    } catch (Exception e) {
    }
    JsonObject asJsonObject = root.getAsJsonObject("data");
    JsonArray array = asJsonObject.getAsJsonArray("cards");
    for (int i = 0; i < array.size(); i++) {
      JsonObject mblog = array.get(i).getAsJsonObject().getAsJsonObject("mblog");
      if (mblog != null) {
        JsonArray pics = mblog.getAsJsonArray("pics");
        if (pics != null) {
          for (int j = 0; j < pics.size(); j++) {
            JsonObject o = pics.get(j).getAsJsonObject();
            JsonObject large = o.getAsJsonObject("large");
            if (large != null) {
              urls.add(large.get("url").getAsString());
            }
          }
        }
      }
    }
    return array.size();
  }

}
