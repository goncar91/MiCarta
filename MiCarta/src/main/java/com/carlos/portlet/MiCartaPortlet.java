package com.carlos.portlet;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.Portlet;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.ProcessAction;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.osgi.service.component.annotations.Component;

import com.carlos.constants.MiCartaPortletKeys;
import com.liferay.document.library.kernel.model.DLFolder;
import com.liferay.document.library.kernel.model.DLFolderConstants;
import com.liferay.document.library.kernel.service.DLAppLocalServiceUtil;
import com.liferay.document.library.kernel.service.DLFileEntryLocalServiceUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCPortlet;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.repository.model.Folder;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.ServiceContextFactory;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.upload.UploadPortletRequest;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;

import MiCartaService.model.Plato;
import MiCartaService.service.PlatoLocalServiceUtil;

/**
 * @author JE10846
 */
@Component(
	immediate = true,
	property = {
		"com.liferay.portlet.display-category=category.sample",
		"com.liferay.portlet.header-portlet-css=/css/main.css",
        "com.liferay.portlet.header-portlet-javascript=/js/main.js",
		"com.liferay.portlet.instanceable=true",
		"javax.portlet.init-param.template-path=/",
		"javax.portlet.init-param.view-template=/view.jsp",
		"javax.portlet.name=" + MiCartaPortletKeys.MiCarta,
		"javax.portlet.resource-bundle=content.Language",
		"javax.portlet.security-role-ref=power-user,user"
		
	},
	service = Portlet.class
)
public class MiCartaPortlet extends MVCPortlet {
	
	private static final Log log = LogFactoryUtil.getLog(MiCartaPortlet.class);
	
	private final String CARPETA_IMAGENES_PLATOS = "platos";
	
	@Override
	public void render(RenderRequest renderRequest, RenderResponse renderResponse)
			throws IOException, PortletException {
		log.info("Render MiCartaPortlet");
		ThemeDisplay themeDisplay = (ThemeDisplay) renderRequest.getAttribute(WebKeys.THEME_DISPLAY);
		try {
			List<Plato> platos = PlatoLocalServiceUtil.getPlatosByUser(themeDisplay.getUserId());
			renderRequest.setAttribute("platos", platos);
		}catch (Exception e) {
			log.error("Error al sacar los platos del usuario: "+themeDisplay.getUserId(),e);
		}
		
		
		super.render(renderRequest, renderResponse);
	}
	
	@ProcessAction(name="borrarPlato")
	public void borrarPlato(ActionRequest request, ActionResponse response) {

		long idAEliminar = ParamUtil.getLong(request, "idPlatoAEliminar",-1);
		log.info("Plato a eliminar: "+idAEliminar);
		if (idAEliminar > 0) {
			try {
				Plato p = PlatoLocalServiceUtil.deletePlato(idAEliminar);
				DLFileEntryLocalServiceUtil.deleteDLFileEntry(new Long(p.getImagen()));
			}catch (Exception e) {
				log.error("Error al eliminar el plato con id: "+idAEliminar,e);
			}
		}
	}
	
	
	
	@ProcessAction(name="crearPlato")
	public void crearPlato(ActionRequest request, ActionResponse response) {
		log.info("Crear plato action");
		ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
		String nombrePlato = ParamUtil.getString(request, "nombrePlato", ""); 
		String descripcionPlato = ParamUtil.getString(request, "descripcionPlato", ""); 
		float precioPlato = ParamUtil.getFloat(request, "precioPlato", 0.0f);
		
		if (nombrePlato.isEmpty() || descripcionPlato.isEmpty() || precioPlato==0.0f) {
			SessionErrors.add(request, "campos.incompletos");
		}else {
			Plato p =null;
			try {
				p = PlatoLocalServiceUtil.crearPlato(themeDisplay.getUserId(), nombrePlato, descripcionPlato, precioPlato, "");
			}catch (Exception e) {
				log.error("Error al crear el plato",e);
			}
			FileEntry imagen = null;
			try {
				UploadPortletRequest uploadPortletRequest = PortalUtil.getUploadPortletRequest(request);
				File uploadedFile = uploadPortletRequest.getFile("imagenPlato");
				if (Validator.isNull(uploadedFile) || uploadedFile.length()==0) {
					SessionErrors.add(request, "no.imagen");
					throw new Exception("No imagen");
				}
				
				String contentType = FileUtil.getExtension(uploadedFile.getPath());
				long repositoryId = DLFolderConstants.getDataRepositoryId(themeDisplay.getUser().getGroupId(), 0);
				ServiceContext serviceContextImg = new ServiceContext();
				serviceContextImg.setScopeGroupId(themeDisplay.getUser().getGroupId());
				//Damos permisos al archivo para usuarios de comunidad.
				serviceContextImg.setAddGroupPermissions(true);
				serviceContextImg.setAddGuestPermissions(true);
				long igFolderId= createIGFolders(request, themeDisplay.getUserId(),repositoryId);
				imagen = DLAppLocalServiceUtil.addFileEntry(themeDisplay.getUserId(), repositoryId , igFolderId ,
						nombrePlato+"."+contentType , contentType, nombrePlato , "", "", uploadPortletRequest.getFile("imagenPlato") , serviceContextImg ); 
				
			}catch (Exception e) {
				log.error("Error al guardar la imagen del plato subida",e);
			}
			
			if (Validator.isNotNull(imagen)) {
				try {
					p.setImagen(""+imagen.getFileEntryId());
					p = PlatoLocalServiceUtil.updatePlato(p);
				}catch (Exception e) {
					log.error("Error al crear el plato",e);
				}
				
			}
		}
		
		
		
	}
	
	private long createIGFolders(PortletRequest request,long userId,long repositoryId) throws PortalException, SystemException{
	    try {
	    	// Si existe la carpeta se devuelve el id
	    	Folder igMainFolder = DLAppLocalServiceUtil.getFolder(repositoryId,DLFolderConstants.DEFAULT_PARENT_FOLDER_ID, CARPETA_IMAGENES_PLATOS);
	    	return igMainFolder.getFolderId();
	    } catch (Exception ex) {
	    	log.error("No se ha encontrado la carpeta: "+CARPETA_IMAGENES_PLATOS);
	    }
	    // Si no existe la carpeta la creamos para la proxima vez
    	ServiceContext serviceContext= ServiceContextFactory.getInstance( DLFolder.class.getName(), request);
		serviceContext.setAddGroupPermissions(true);
		serviceContext.setAddGuestPermissions(true);
    	Folder newImageMainFolder=DLAppLocalServiceUtil.addFolder(userId, repositoryId, 0, CARPETA_IMAGENES_PLATOS, "Carpeta para las imagenes de los platos", serviceContext);
    	return newImageMainFolder.getFolderId();
	}
	
}