package burp;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import custom.YunSu;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import custom.GUI;
import custom.myYunSu;
import custom.imageType;

public class BurpExtender implements IBurpExtender, ITab, IContextMenuFactory, IIntruderPayloadGeneratorFactory,IIntruderPayloadGenerator
{	
	private GUI GUI;
    private static IBurpExtenderCallbacks callbacks;
    private IExtensionHelpers helpers;
    
    public PrintWriter stdout;//�������ﶨ�����������registerExtenderCallbacks������ʵ������������ں����о�ֻ�Ǿֲ���������������ʵ��������ΪҪ�õ�����������
    private String ExtenderName = "reCAPTCHA v0.1 by bit4";
    private String github = "https://github.com/bit4woo/reCAPTCHA";
	
	private String imgName;
    public IHttpRequestResponse imgMessageInfo;
    
    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks)
    {
    	stdout = new PrintWriter(callbacks.getStdout(), true);
    	stdout.println(ExtenderName);
    	stdout.println(github);
        this.callbacks = callbacks;
        helpers = callbacks.getHelpers();
        callbacks.setExtensionName(ExtenderName); //�������
        //callbacks.registerHttpListener(this); //���û��ע�ᣬ�����processHttpMessage�����ǲ�����Ч�ġ������������Ӧ���Ĳ�������Ӧ���Ǳ�Ҫ��
        callbacks.registerContextMenuFactory(this);
        callbacks.registerIntruderPayloadGeneratorFactory(this);
        addMenuTab();        
    }

/////////////////////////////////////////�Զ��庯��/////////////////////////////////////////////////////////////
    public static IBurpExtenderCallbacks getBurpCallbacks() {
        return callbacks;
    }
    
    public static byte[] subBytes(byte[] src, int begin, int count) {
        byte[] bs = new byte[count];
        for (int i=begin; i<begin+count; i++) bs[i-begin] = src[i];
        return bs;
    }
    
	public String getHost(IRequestInfo analyzeRequest){
    	List<String> headers = analyzeRequest.getHeaders();
    	String domain = "";
    	for(String item:headers){
    		if (item.toLowerCase().contains("host")){
    			domain = new String(item.substring(6));
    		}
    	}
    	return domain ;
	}
	
	public String getImage(IHttpRequestResponse messageInfo) {
		if (messageInfo != null) {
			IHttpService service = messageInfo.getHttpService();
			byte[] request =  messageInfo.getRequest();
			IHttpRequestResponse messageInfo_issued = callbacks.makeHttpRequest(service,request);
			
			byte[] response = messageInfo_issued.getResponse();
			int BodyOffset = helpers.analyzeResponse(response).getBodyOffset();
			int body_length = response.length -BodyOffset;
			byte[] body = subBytes(response,BodyOffset,body_length);
			//����֮ǰ����һ���ӣ��ֽ�byte[]ת��Ϊstring��ȡsubstring��ת��������������������ġ�
			//stdout.println("Response length:");
			//stdout.println(response.length);
			//stdout.println("offset");
			//stdout.println(BodyOffset);
			//stdout.println("body length");
			//stdout.println(body.length);
		
		    imgName = getHost(helpers.analyzeRequest(messageInfo))+System.currentTimeMillis();
		    //stdout.println(imgName);
		    try {
		    	File imageFile = new File(imgName);
		        //���������  
		        FileOutputStream outStream = new FileOutputStream(imageFile);  
		        //д������  
				outStream.write(body);
				outStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		    String type = imageType.getPicType(imgName);
		    String newName = null;
		    if(type.equals("unknown")) {
		    	newName =imgName +"jpg";
		    }else {
		    	newName = imgName +type;
		    }
		    
		    File oldfile = new File(imgName);
		    File newfile = new File(newName);
		    oldfile.renameTo(newfile);
		    //String newFileName = newfile.getName();
		    
            return newName;
		}
		else {
			return null;
		}
	}
	
///////////////////////////////////�Զ��庯��////////////////////////////////////////////////////////////
	
	
///////////////////////////////////�����Ǹ���burp����ķ��� --start//////////////////////////////////////////
    public void addMenuTab()
    {
      SwingUtilities.invokeLater(new Runnable()
      {
        public void run()
        {
          BurpExtender.this.GUI = new GUI();
          BurpExtender.this.callbacks.addSuiteTab(BurpExtender.this); //�����BurpExtender.thisʵ����ָITab����Ҳ����getUiComponent()�е�contentPane.���������CGUI()������ʼ����
          //������ﱨjava.lang.NullPointerException: Component cannot be null ������Ҫ�Ų�contentPane�ĳ�ʼ���Ƿ���ȷ��
        }
      });
    }
	
	
    //ITab����ʵ�ֵ���������
	@Override
	public String getTabCaption() {
		// TODO Auto-generated method stub
		return ("reCAPTCHA");
	}
	@Override
	public Component getUiComponent() {
		// TODO Auto-generated method stub
		return this.GUI;
	}

	
	@Override
	public List<JMenuItem> createMenuItems(IContextMenuInvocation invocation)
	{ //��Ҫ��ǩ��ע�ᣡ��callbacks.registerContextMenuFactory(this);
	    IHttpRequestResponse[] messages = invocation.getSelectedMessages();
	    List<JMenuItem> list = new ArrayList<JMenuItem>();
	    if((messages != null) && (messages.length ==1))
	    {	
	    	imgMessageInfo = messages[0];
	    	
	    	final byte[] sentRequestBytes = messages[0].getRequest();
	    	IRequestInfo analyzeRequest = helpers.analyzeRequest(sentRequestBytes);
	    	
	        JMenuItem menuItem = new JMenuItem("Send to reCAPTCHA");
	        menuItem.addActionListener(new ActionListener()
	        {
	          public void actionPerformed(ActionEvent e)
	          {
	            try
	            {	
	            	//stdout.println(new String(imgMessageInfo.getRequest()));
	            	GUI.MessageInfo = imgMessageInfo;
	            	
	            	GUI.imgRequestRaws.setText(new String(imgMessageInfo.getRequest())); //��GUI����ʾ���������Ϣ��
	            	
	            	//IHttpService httpservice =imgMessageInfo.getHttpService();
	            	//String host = httpservice.getHost();
	            	//int port = httpservice.getPort();
	            	//String protocol = httpservice.getProtocol();
	            	
	            	
	            	GUI.imgHttpService.setText(imgMessageInfo.getHttpService().toString());
	            	
	            }
	            catch (Exception e1)
	            {
	                BurpExtender.this.callbacks.printError(e1.getMessage());
	            }
	          }
	        });
	        list.add(menuItem);
	    }
	    return list;
	}
	
	
	//IIntruderPayloadGeneratorFactory ����ʵ�ֵ�2������
	@Override
	public String getGeneratorName() {
		// TODO Auto-generated method stub
		return "reCAPTCHA";
	}

	@Override
	public IIntruderPayloadGenerator createNewInstance(IIntruderAttack attack) {
		// TODO Auto-generated method stub
		return this;
	}
	
	
	
	//IIntruderPayloadGenerator ����ʵ�ֵ���������
	@Override
	public boolean hasMorePayloads() {
		// TODO Auto-generated method stub
		return true;
	}
	
	@Override
	public byte[] getNextPayload(byte[] baseValue) {
		// ��ȡͼƬ��֤���ֵ
		int times = 0;
		while(times <=5) {
			if (imgMessageInfo!=null) {
				String imgpath = getImage(imgMessageInfo);
				String paraString = GUI.APIRequestRaws.getText();
				String code = myYunSu.getCode(imgpath,paraString);
				stdout.println(imgpath+" "+code);
				return code.getBytes();
			}
			else {
				stdout.println("Failed try!!! please send image request to reCAPTCHA first!");
				times +=1;
				continue;
			}
		}
		return null;
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}
	
//////////////////////////////////////////////����burp����ķ��� --end//////////////////////////////////////////////////////////////
}