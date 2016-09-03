package com.taobao.rigel.rap.auto.generate.bo;

import com.taobao.rigel.rap.auto.generate.bo.GenerateUtils.GeneratorType;
import com.taobao.rigel.rap.auto.generate.bo.GenerateUtils.TargetObjectType;
import com.taobao.rigel.rap.auto.generate.contract.Generator;
import com.taobao.rigel.rap.project.bo.Action;
import com.taobao.rigel.rap.project.bo.Page;
import com.taobao.rigel.rap.project.bo.Parameter;
import com.taobao.rigel.rap.project.bo.Project.STAGE_TYPE;

public class VelocityTemplateGenerator implements Generator {
	private Page page;
	
	@Override
	public boolean isAvailable(STAGE_TYPE stage) {
		/**
		 * will be available on all stages
		 */
		return true;
	}

	@Override
	public GeneratorType getGeneratorType() {
		return GeneratorType.EXPORT_FILE;
	}

	@Override
	public String getAuthor() {
		return "Bosn Ma";
	}

	@Override
	public String getIntroduction() {
		return "该功能可根据项目定义导出Velocity前端模板，所有模板文件会将约定好（在接口文档中）的变量以清晰、"
			+ "直观、彩色化的格式展现出来，使得后端开发人员能够在前端开发人员完成模板编辑之前进行单元自测。";
	}

	@Override
	public String doGenerate() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n");
		stringBuilder.append("<html>\n");
		stringBuilder.append("<head>\n");
		stringBuilder.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />");
		stringBuilder.append("<title>Generated By RAP</title>\n");
		stringBuilder.append("<style type=\"text/css\">\n");
		stringBuilder.append("body {background-color:#E6E6E6; font-size:12px; font-family:Arial,Helvetica,sans-serif;}\n");
		stringBuilder.append(".div-a .head {background-color: Silver;text-align:center;}");
		stringBuilder.append(".div-a .dataType { text-align:center; }\n");
		stringBuilder.append(".tr-p {height: 25px;}\n");
		stringBuilder.append("h1, h2, h3 {color:#8CB70E; font-weight:normal; margin:0; text-transform: uppercase;}");
		stringBuilder.append(".td-p {border: Gray 1px solid;border-collapse: collapse;padding: 5px;}\n");
		stringBuilder.append(".table-a {border: Gray 1px solid;border-collapse: collapse;margin: 12px 8px 8px 8px;}\n");
		stringBuilder.append(".div-a { margin: 12px;}\n");
		stringBuilder.append(".div-a .head { background-color: Silver; text-align:center; }\n");
		stringBuilder.append(".div-a .head-name { width: 120px }\n");
		stringBuilder.append(".div-a .head-validator { width: 80px; }\n");
		stringBuilder.append(".div-a .head-type { width: 60px; }\n");
		stringBuilder.append(".div-a .head-remark { width: 200px; }\n");
		stringBuilder.append(".div-a .head-identifier { width: 140px; }\n");
		stringBuilder.append(".div-a .name { } \n");
		stringBuilder.append(".tester-form {margin: 12px; padding:8px; border: 1px gray dashed;} \n");
		stringBuilder.append(" .item { padding:8px;} \n");
		stringBuilder.append(" .identifier { color:Red;} \n");
		stringBuilder.append(" .url { color:Silver;} \n");
		stringBuilder.append(".div-a .validator { color: Red; text-align:center; }\n");
		stringBuilder.append(".div-a .real { color: Red; text-align:center; }\n");
		stringBuilder.append(".div-a .remark { color: Blue; }\n");
		stringBuilder.append(".div-a .identifier { color: Red; }\n");
		stringBuilder.append("input, textarea{ border:1px solid #CCCCCC; }\n");
		stringBuilder.append("</style>");		
		stringBuilder.append("</head>\n");
		stringBuilder.append("<body>\n");
		stringBuilder.append("<h2>ABOUT TEMPLATE</h2>\n页面名称: " + page.getName() + "<br />");
		stringBuilder.append("页面编号: " + page.getId() + "<br />");
		stringBuilder.append("页面介绍: " + page.getIntroduction() + "<br />");
		stringBuilder.append("<br /><hr /><br />");
		
		stringBuilder.append("<font color='gray'><H2> RESPONSE PARAMETER LIST</H2></font>");
		stringBuilder.append("<div class=\"div-a\">");
		stringBuilder.append("<table class=\"table-a\"><tr class=\"head\">");
		stringBuilder.append("<td class=\"head-identifier\">变量名</td><td class=\"head-name\">参数意义</td>");
		stringBuilder.append("<td class=\"head-type\">参数类型</td><td class=\"head-identifier\">实际传值</td>");
		stringBuilder.append("<td class=\"head-remark\">备注</td></tr>");
		

		// print response parameter list
		for (Action action : page.getActionList()) {
			if (!action.getResponseTemplate().equals(page.getTemplate())) continue;			
			for (Parameter p : action.getResponseParameterList()) {
				stringBuilder.append("<tr>");
				stringBuilder.append("<td class=\"td-p identifier\">" + p.getIdentifier() + "</td>");
				stringBuilder.append("<td class=\"td-p name\">" + p.getName() + "</td>");
				stringBuilder.append("<td class=\"td-p dataType\">" + p.getDataType() + "</td>");
				stringBuilder.append("<td class=\"td-p real\">$!" + p.getIdentifier() + "</td>");
				stringBuilder.append("<td class=\"td-p remark\">" + p.getRemark() + "</td>");
				stringBuilder.append("</tr>");
			}			
		}	
		stringBuilder.append("</table>\n</div>\n<div>");	
		int formCount = 1;
		
		// generate action tester
		for (Action action : page.getActionList()) {
			stringBuilder.append("<h2> Action: " + action.getName() + " Tester</h2>");
			stringBuilder.append("<div class=\"tester-form\"><form name=\"formTester" + (formCount++) + "\" method=\"post\" action=\"" + action.getRequestUrl() + "\">");
			stringBuilder.append("<div class=\"item\">请求地址：" + action.getRequestUrl() + "</div>");
			for (Parameter p : action.getRequestParameterList()) {
				stringBuilder.append("<div class=\"item\"><input type=\"text\" width=\"200px\" name=\"" 
						+ p.getIdentifier() + "\"/>&nbsp; &nbsp;变量名: <font color='red'>" + p.getIdentifier() + "</font> &nbsp;&nbsp;变量意义: <font color='gray'>" 
						+ p.getName() + "</font>&nbsp;&nbsp;备注: <font color='blue'>"	+ p.getRemark() + "</font></div>");
			}
			stringBuilder.append("<div class=\"item\"><input type=\"submit\" class=\"button\" value=\"Test\"/></div>");
			stringBuilder.append("</form></div>");
		}
		stringBuilder.append("</div>");
		stringBuilder.append("</body>\n");
		stringBuilder.append("</html>\n");
		return stringBuilder.toString();
	}

	@Override
	public TargetObjectType getTargetObjectType() {
		return TargetObjectType.PAGE;
	}

	@Override
	public void setObject(Object obj) {
		this.page = (Page) obj;
		
	}

}
