<%--
  Created by IntelliJ IDEA.
  User: bieber
  Date: 2015/6/4
  Time: 0:36
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@include file="common/header.jsp"%>
<div class="container-fluid " ng-controller="dubbokeeperCtrl">
    <div class="row">
        <div class="col-md-2 col-lg-2 col-xs-2" ng-show="hasMenu">
            <div class="row">
                <div class="col-md-12 col-lg-12 col-xs-12">
                    <menu-tpl></menu-tpl>
                </div>
            </div>
        </div>
        <div class="{{hasMenu?'col-md-10 col-lg-10 col-xs-10':'col-md-12 col-lg-12 col-xs-12'}} app-container">
            <breadcrumb-tpl ng-show="needBreadCrumb"></breadcrumb-tpl>
            <div class="row">
                <div class="col-md-12 col-lg-12 col-xs-12" ng-view>
                </div>
            </div>
        </div>
    </div>
</div>
<dialog-tpl></dialog-tpl>
<%@include file="common/scripts.jsp"%>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/modules/theme/theme.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/modules/router/router.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/modules/override/override.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/modules/statistics/statistics.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/modules/monitor/monitor.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/modules/zoopeeper/zoopeeper.js"></script>
<!-- <script type="text/javascript" src="${pageContext.request.contextPath}/js/modules/common/filter.js"></script>-->
<script type="text/javascript" src="${pageContext.request.contextPath}/js/modules/common/basic.module.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/modules/common/http.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/modules/common/stickup.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/modules/common/dialog.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/modules/common/echarts.js"></script>
<!-- <script type="text/javascript" src="${pageContext.request.contextPath}/js/modules/common/echarts.js"></script> -->
<script type="text/javascript" src="${pageContext.request.contextPath}/js/modules/common/fullscreen.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/modules/common/apps-dependencies.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/modules/common/date-range-picker.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/modules/breadcrumb/breadcrumb.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/modules/apps/provider.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/modules/apps/apps.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/modules/head/head.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/modules/menu/menu.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/modules/aboutus/aboutus.js"></script>
<!--  for hydra query start -->
<script type="text/javascript" src="${pageContext.request.contextPath}/js/modules/hydraquery/repository/trace-repo.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/modules/hydraquery/repository/service-repo.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/modules/hydraquery/service/sequence-service.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/modules/hydraquery/service/tree-service.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/modules/hydraquery/service/query-service.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/modules/hydraquery/filters.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/modules/hydraquery/query.js"></script>
<!--  for hydra query end -->
<!--  for hydra query start -->
<script language="javascript" type="text/javascript">
	var ctp = "<%=request.getContextPath() %>";
</script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/libs/d3.v3.min.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/libs/jquery.qtip.min.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/libs/datetimepicker/js/bootstrap-datetimepicker.min.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/js/libs/datetimepicker/js/locales/bootstrap-datetimepicker.zh-CN.js"></script>
<!--  for hydra query end -->	
<script type="text/javascript" src="${pageContext.request.contextPath}/js/modules/index.js"></script>
<!--  for hydra query start -->
<script type="text/javascript" src="${pageContext.request.contextPath}/js/echarts/echarts.js"></script>
<script type="text/javascript">
    // 路径配置
    require.config({
        paths: {
            echarts: 'js/echarts'
        }
    });
</script>
<!--  for hydra query end -->	

<%@include file="common/footer.jsp"%>

