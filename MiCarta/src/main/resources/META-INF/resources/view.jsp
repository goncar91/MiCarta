<%@page import="java.util.List"%>
<%@page import="com.liferay.document.library.kernel.model.DLFileEntry"%>
<%@page import="MiCartaService.model.Plato"%>
<%@page import="com.liferay.document.library.kernel.service.DLFileEntryLocalServiceUtil"%>
<%@page import="com.liferay.document.library.kernel.exception.FileEntryLockException"%>
<%@page import="com.liferay.portal.kernel.repository.model.FileEntry"%>
<%@page import="java.io.File"%>
<%@ include file="init.jsp" %>


<liferay-ui:error key="campos.incompletos" message="error.campos.incompletos"/>
<liferay-ui:error key="no.imagen" message="error.no.imagen"/>

<liferay-portlet:actionURL  name="crearPlato" var="crearPlatoURL"></liferay-portlet:actionURL> 
<div class="contenedor-micarta">
	<div class="form-subirplatos">
		<aui:form enctype="multipart/form-data"  method="POST" action="${crearPlatoURL}" name="editarImagenForm" >
			<aui:input name="nombrePlato" label="micarta.misplatos.nombre"/>
			<aui:input name="precioPlato" label="micarta.misplatos.precio" type="number" step="0.01"/>
			<aui:input name="descripcionPlato" label="micarta.misplatos.descripcion"/>
		
			
			<div>
				<aui:input id="imagenPlato" type="file" name="imagenPlato" label="micarta.misplatos.imagen"/>
				<img src="${plato.getImagen()}">
			</div>
			
			
			
			<aui:button type="submit" value="micarta.misplatos.subirplato" />
		</aui:form>
	
	</div>
	<div class="contenedor-platos">
		<div>
			<liferay-ui:message key="micarta.misplatos"/>
		</div>
		<%
			List<Plato> platos = (List<Plato>) request.getAttribute("platos");
			for(Plato plato : platos){
				DLFileEntry file = DLFileEntryLocalServiceUtil.getFileEntry(Long.parseLong(plato.getImagen()));
				pageContext.setAttribute("idPLato",plato.getId());
		%>
		
		
			<liferay-ui:message key="micarta.misplatos.nombre"/>: <%=plato.getNombre()%><br/>
			<liferay-ui:message key="micarta.misplatos.precio"/>: <%=plato.getPrecio()%><br/>
			<liferay-ui:message key="micarta.misplatos.descripcion"/>: <%=plato.getDescripcion()%><br/>
		
			
			
			<img src="<%=themeDisplay.getPortalURL() + themeDisplay.getPathContext() + "/documents/" + themeDisplay.getScopeGroupId() + "/" + 
		                                  file.getFolderId() +  "/" +file.getTitle()%>">
			
			<liferay-portlet:actionURL name="borrarPlato" var="borrarPlatoURL">
				<liferay-portlet:param name="idPlatoAEliminar" value="<%=String.valueOf(plato.getId())%>"/>
			</liferay-portlet:actionURL>
			<aui:form action="${borrarPlatoURL}" method="POST">
				<aui:button type="submit" value="micarta.misplatos.eliminar" />
			</aui:form>	
			
		<%
			}
		%>
	</div>
</div>
