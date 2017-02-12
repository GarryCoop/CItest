package com.great.eastern.document.merging.controller;

import com.great.eastern.model.dto.DocumentsDTO;
import com.great.eastern.model.dto.FoldersDTO;
import com.liferay.counter.service.CounterLocalServiceUtil;
import com.liferay.documentlibrary.NoSuchFileException;
import com.liferay.portal.kernel.cache.CacheRegistryUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HtmlUtil;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.GroupConstants;
import com.liferay.portal.model.ResourceConstants;
import com.liferay.portal.model.ResourcePermission;
import com.liferay.portal.model.Role;
import com.liferay.portal.model.RoleConstants;
import com.liferay.portal.model.User;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.ResourcePermissionLocalServiceUtil;
import com.liferay.portal.service.RoleLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.ServiceContextFactory;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.PortalPreferences;
import com.liferay.portlet.PortletPreferencesFactoryUtil;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.model.DLFolder;
import com.liferay.portlet.documentlibrary.model.DLFolderConstants;
import com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.DLFolderLocalServiceUtil;
import com.liferay.util.TextFormatter;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.portlet.bind.annotation.ActionMapping;
import org.springframework.web.portlet.bind.annotation.RenderMapping;

@Controller("documentMergingController")
@RequestMapping("VIEW")
public class DocumentMergingController {
	
	private static Log log=LogFactoryUtil.getLog(DocumentMergingController.class);
	private static int RECURSIVE_LIMIT=100;
	private static long defaultCompanyId=PortalUtil.getDefaultCompanyId();

	@RenderMapping
	public String defaultRender(RenderRequest request, RenderResponse response, Model model){
		ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
		User user = themeDisplay.getUser();
		
		try {
			List<Group> groups=GroupLocalServiceUtil
				.search(themeDisplay.getCompanyId(), null, null, null, -1, -1);
			
			model.addAttribute("groups", groups);
		} catch (Exception e) {
			SessionErrors.add(request, "merging.failed");
			model.addAttribute("errorMessage", "Initializing document merging failed");
			e.printStackTrace();
		}
		
		return "view";
	}
	
	@RenderMapping(params="action=changeCommunitySource")
	public String changeCommunitySource(RenderRequest request, RenderResponse response, Model model){
		ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
		User user = themeDisplay.getUser();
		
		Long selectedGroupId=ParamUtil.getLong(request, "selectedGroupId");
		Long destinationGroupId=ParamUtil.getLong(request, "destinationGroupId");
		try {
			List<Group> groups=GroupLocalServiceUtil
				.search(themeDisplay.getCompanyId(), null, null, null, -1, -1);
			model.addAttribute("groups", groups);
			model.addAttribute("selectedGroupId", selectedGroupId);
			model.addAttribute("destinationGroupId", destinationGroupId);
			
			if(selectedGroupId!=null && selectedGroupId!=0 &&
					destinationGroupId!=null && destinationGroupId!=0){
				List<DLFolder> folders= DLFolderLocalServiceUtil
					.getFolders(selectedGroupId, DLFolderConstants.DEFAULT_PARENT_FOLDER_ID);
				List<DLFileEntry> fes=DLFileEntryLocalServiceUtil
					.getFileEntries(selectedGroupId, DLFolderConstants.DEFAULT_PARENT_FOLDER_ID);
				List<FoldersDTO> fsd=new ArrayList<FoldersDTO>();
				List<DocumentsDTO> dsd=new ArrayList<DocumentsDTO>();
				
				castToDTO(folders, fes, fsd, dsd, selectedGroupId, themeDisplay);
				
				model.addAttribute("folders", fsd);
				model.addAttribute("documents", dsd);
				model.addAttribute("backUrl", themeDisplay.getURLCurrent());
			}
		} catch (Exception e) {
			SessionErrors.add(request, "merging.failed");
			model.addAttribute("errorMessage", "Searching folders/documents failed");
			e.printStackTrace();
		}
		
		return "view";
	}
	
	public void castToDTO(List<DLFolder> folders, List<DLFileEntry> fes,
			List<FoldersDTO> fsd,List<DocumentsDTO> dsd, Long selectedGroupId,
			ThemeDisplay themeDisplay) throws Exception{
		int foldersCount=0;
		int documentsCount=0;
		for(DLFolder dl:folders){
			FoldersDTO fd=new FoldersDTO();
			fd.setId(dl.getFolderId());
			fd.setName(dl.getName());
			
			foldersCount=DLFolderLocalServiceUtil
				.getFoldersCount(selectedGroupId, dl.getFolderId());
			fd.setFoldersCount(foldersCount);
			
			documentsCount=DLFolderLocalServiceUtil
				.getFileEntriesAndFileShortcutsCount(selectedGroupId, dl.getFolderId(), WorkflowConstants.STATUS_APPROVED);
			fd.setDocumentsCount(documentsCount);
			
			StringBuilder sb=new StringBuilder("/group/control_panel/manage/-/document_library/view/");
			sb.append(dl.getFolderId());
			sb.append("?p_p_state=maximized&doAsGroupId=");
			sb.append(dl.getGroupId());
			sb.append("&refererPlid=");
			sb.append(themeDisplay.getPlid());
			sb.append("&_20_redirect=");
			sb.append(themeDisplay.getURLCurrent());
			fd.setUrl(sb.toString());
			
			fsd.add(fd);
		}
		
		for(DLFileEntry fe:fes){
			DocumentsDTO dd=new DocumentsDTO();
			dd.setId(fe.getFileEntryId());
			dd.setIcon(fe.getIcon());
			dd.setName(fe.getTitle());
			
			String size=TextFormatter.formatKB(fe.getSize(), themeDisplay.getLocale()) + "k";
			dd.setSize(size);
			
			dd.setDownloads(fe.getReadCount());
			dd.setLocked(fe.isLocked());
			
			StringBuilder sb=new StringBuilder("/group/control_panel/manage/-/document_library/view/");
			sb.append(fe.getFolderId());
			sb.append(StringPool.SLASH);
			sb.append(HtmlUtil.unescape(fe.getName()));
			sb.append("?p_p_state=maximized&doAsGroupId=");
			sb.append(fe.getGroupId());
			sb.append("&refererPlid=");
			sb.append(themeDisplay.getPlid());
			sb.append("&_20_redirect=");
			sb.append(HttpUtil.encodeURL(themeDisplay.getURLCurrent()));
			dd.setUrl(sb.toString());
			
			dsd.add(dd);
		}
	}
	
	@ActionMapping(params="action=submitFormMerging")
	public void submitFormMerging(ActionRequest request, ActionResponse response, Model model ){
		ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
		User user = themeDisplay.getUser();
		
		Long selectedGroupId=ParamUtil.getLong(request, "from");
		Long destinationGroupId=ParamUtil.getLong(request, "to");
		long[] folders=ParamUtil.getLongValues(request, "selectedFolders");
		long[] documents=ParamUtil.getLongValues(request, "selectedDocuments");
		Boolean isFolder=ParamUtil.getBoolean(request, "isFolder");
		String backUrl=ParamUtil.getString(request, "backUrl");
		Group srcGroup=null;
		Group destGroup=null;
		try {
			ServiceContext serviceContext=ServiceContextFactory
				.getInstance(DLFileEntry.class.getName(), request);
			
			ServiceContext serviceContext1=ServiceContextFactory
			.getInstance(DLFolder.class.getName(), request);
			
			PortalPreferences preferences =PortletPreferencesFactoryUtil
				.getPortalPreferences(request);
			long defaultFolderId = GetterUtil.getLong(
				preferences.getValue("rootFolderId", StringPool.BLANK),
				DLFolderConstants.DEFAULT_PARENT_FOLDER_ID
			);
			Group globalGroup=GroupLocalServiceUtil.getCompanyGroup(defaultCompanyId);

			srcGroup=GroupLocalServiceUtil.getGroup(selectedGroupId);
			destGroup=GroupLocalServiceUtil.getGroup(destinationGroupId);
			if(selectedGroupId.compareTo(destinationGroupId)==0){
				SessionErrors.add(request, "merging.failed");
				model.addAttribute("errorMessage", "Files cannot be merged within the same community");
			}else if(isFolder && folders.length>0){
				List<DLFolder> tempFolders=new ArrayList<DLFolder>();
				List<DLFolder> tempSubFolders=new ArrayList<DLFolder>();
				Map<Long,Long> mapFolder=new HashMap<Long,Long>();
				for(long fd:folders){
					log.info("============start fetch sub/child folder=============");
					System.out.println("============start fetch sub/child folder=============");
					DLFolder rootFolder=DLFolderLocalServiceUtil.getDLFolder(fd);
					System.out.print("|"+rootFolder.getName()+" check, create folder|");
					checkAndCreateDestinationFolder(destinationGroupId,defaultFolderId,
						rootFolder,mapFolder,serviceContext1);
					
					List<DLFileEntry> rootFileEntries=DLFileEntryLocalServiceUtil
						.getFileEntries(selectedGroupId, rootFolder.getFolderId());
					long newRootFolderId=mapFolder.get(rootFolder.getFolderId());
					if(rootFileEntries!=null && rootFileEntries.size()>0){
						for(DLFileEntry rootFe:rootFileEntries){
							log.info(">"+rootFolder.getName()+" - "+rootFe.getTitle()+" check, create file<");
							System.out.println(">"+rootFolder.getName()+" - "+rootFe.getTitle()+" check, create file<");
							checkAndCreateDestinationDocument(destinationGroupId,newRootFolderId,
								rootFe,serviceContext,globalGroup.getGroupId());
						}
					}
					System.out.println();
					
					for(int i=1;i<=RECURSIVE_LIMIT;i++){
						if(i==1){
							tempFolders=DLFolderLocalServiceUtil.getFolders(selectedGroupId, fd);
						}else{
							tempFolders=tempSubFolders;
							tempSubFolders=new ArrayList<DLFolder>();
						}
						
						for(DLFolder dl:tempFolders){
							log.info("|"+dl.getName()+" check, create, and get subfolder|");
							System.out.println("|"+dl.getName()+" check, create, and get subfolder|");
							long newParentFolderId=mapFolder.get(dl.getParentFolderId());
							checkAndCreateDestinationFolder(destinationGroupId,newParentFolderId,
								dl,mapFolder,serviceContext1);
							
							List<DLFolder> subFolders=DLFolderLocalServiceUtil
								.getFolders(selectedGroupId, dl.getFolderId());
							tempSubFolders.addAll(subFolders);
							
							List<DLFileEntry> fileEntries=DLFileEntryLocalServiceUtil
								.getFileEntries(selectedGroupId, dl.getFolderId());
							long newFolderId=mapFolder.get(dl.getFolderId());
							if(fileEntries!=null && fileEntries.size()>0){
								for(DLFileEntry fe:fileEntries){
									log.info(">"+dl.getName()+" - "+fe.getTitle()+" check, create file<");
									System.out.println(">"+dl.getName()+" - "+fe.getTitle()+" check, create file<");
									checkAndCreateDestinationDocument(destinationGroupId,newFolderId,
										fe,serviceContext,globalGroup.getGroupId());

									//After all files moved, delete it
									DLFileEntryLocalServiceUtil.deleteDLFileEntry(fe);
								}
							}
						}
						System.out.println();
						
						if(tempSubFolders==null || (tempSubFolders!=null && tempSubFolders.size()==0)){
							log.info("============no sub/child folder exist=============");
							System.out.println("============no sub/child folder exist=============");
							break;
						}
					}
					
					//After all subfolders and files moved, delete it
					DLFolderLocalServiceUtil.deleteDLFolder(fd);
				}
				
				SessionMessages.add(request, "merging.success");
				model.addAttribute("successMessage", "Success merging folders from "+
					srcGroup.getName()+" to "+destGroup.getName());
			}else if(documents.length>0){
				for(long doc:documents){
					DLFileEntry fe=DLFileEntryLocalServiceUtil.getDLFileEntry(doc);
					checkAndCreateDestinationDocument(destinationGroupId,defaultFolderId,
						fe,serviceContext,globalGroup.getGroupId());

					//After all files moved, delete it
					DLFileEntryLocalServiceUtil.deleteDLFileEntry(doc);
				}
				
				SessionMessages.add(request, "merging.success");
				model.addAttribute("successMessage", "Success merging documents from Community "+
						srcGroup.getName()+" to Community "+destGroup.getName());
			}else{
				SessionErrors.add(request, "merging.failed");
				model.addAttribute("errorMessage", "Please tick at least one folder/document to merge");
			}
			
			List<Group> groups=GroupLocalServiceUtil
				.search(themeDisplay.getCompanyId(), null, null, null, -1, -1);
			model.addAttribute("groups", groups);
			model.addAttribute("selectedGroupId", selectedGroupId);
			model.addAttribute("destinationGroupId", destinationGroupId);
			
			if(selectedGroupId!=null && selectedGroupId!=0 &&
					destinationGroupId!=null && destinationGroupId!=0){
				List<DLFolder> fds= DLFolderLocalServiceUtil
					.getFolders(selectedGroupId, DLFolderConstants.DEFAULT_PARENT_FOLDER_ID);
				List<DLFileEntry> fes=DLFileEntryLocalServiceUtil
					.getFileEntries(selectedGroupId, DLFolderConstants.DEFAULT_PARENT_FOLDER_ID);
				List<FoldersDTO> fsd=new ArrayList<FoldersDTO>();
				List<DocumentsDTO> dsd=new ArrayList<DocumentsDTO>();
				
				castToDTO(fds, fes, fsd, dsd, selectedGroupId, themeDisplay);
				
				model.addAttribute("folders", fsd);
				model.addAttribute("documents", dsd);
				model.addAttribute("backUrl", backUrl);
			}
			
			//clear permission cache after update
			CacheRegistryUtil.clear();
			
//			response.sendRedirect(backUrl);
		} catch (Exception e) {
			SessionErrors.add(request, "merging.failed");
			model.addAttribute("errorMessage", "Error while merging documents from Community "+
				srcGroup.getName()+" to Community "+destGroup.getName()+": "+
				e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void checkAndCreateDestinationFolder(long destinationGroupId,
			long newParentFolderId, DLFolder folder, Map<Long,
			Long> mapFolder, ServiceContext serviceContext) throws Exception{
		DLFolder existingFolder=null;
		try{
			existingFolder=DLFolderLocalServiceUtil
				.getFolder(destinationGroupId, newParentFolderId, folder.getName());
			if(existingFolder!=null){
				mapFolder.put(folder.getFolderId(), existingFolder.getFolderId());
				log.info("folder with name "+folder.getName()+" exist");
				System.out.println("folder with name "+folder.getName()+" exist");
			}
		}catch(Exception e){
			log.info("folder with name "+folder.getName()+
				" not exist in destination community, system will create it");
			System.out.println("folder with name "+folder.getName()+
				" not exist in destination community, system will create it");
		}
		
		if(existingFolder==null){
			DLFolder newFolder=DLFolderLocalServiceUtil.addFolder(
				folder.getUserId(), destinationGroupId,
				newParentFolderId, folder.getName(),
				folder.getDescription(), serviceContext);
				
			mapFolder.put(folder.getFolderId(), newFolder.getFolderId());
			
			log.info(">>>>>>> create newfolder "+
					newFolder.getName()+" | "+newFolder.toString());
			System.out.println(">>>>>>> create newfolder "+
				newFolder.getName()+" | "+newFolder.toString());
			
			movePermission(DLFolder.class.getName(),folder.getCompanyId(),folder.getGroupId(),
				folder.getFolderId(),newFolder.getCompanyId(),newFolder.getGroupId(),newFolder.getFolderId());
		}
	}
	
	private void checkAndCreateDestinationDocument(long destinationGroupId,
			long newParentFolderId, DLFileEntry fileEntry,
			ServiceContext serviceContext, long globalGroupId) throws Exception{
		DLFileEntry existingFileEntry=null;
		try{
			existingFileEntry=DLFileEntryLocalServiceUtil
				.getFileEntryByTitle(destinationGroupId, newParentFolderId, fileEntry.getTitle());
		}catch(Exception e){
			log.info("file with name "+fileEntry.getTitle()+" not exist in destination community, system will create it");
			System.out.println("file with name "+fileEntry.getTitle()+" not exist in destination community, system will create it");
		}
		
		if(fileEntry!=null){
			InputStream is=null;
			try{
				is=DLFileEntryLocalServiceUtil
					.getFileAsStream(fileEntry.getCompanyId(), fileEntry.getUserId(),
						fileEntry.getGroupId(), fileEntry.getFolderId(),
						fileEntry.getName(), fileEntry.getVersion());
			}catch(Exception e){
				is=null;
			}
			
			if(is==null){
				log.info("no physical file exist with id "
					+fileEntry.getFileEntryId()+" and name "+fileEntry.getName());
				System.out.println("no physical file exist with id "
					+fileEntry.getFileEntryId()+" and name "+fileEntry.getName());
				throw new NoSuchFileException("no physical file exist with id "
					+fileEntry.getFileEntryId()+" and name "+fileEntry.getName());
			}
			
			String name=fileEntry.getTitle();
			if(existingFileEntry!=null){
				String extension = FileUtil.getExtension(name);
				String fileName=name.replace((".").concat(extension), "");
				Group fromGroup=GroupLocalServiceUtil.getGroup(fileEntry.getGroupId());
				name=fileName.concat("_").concat(fromGroup.getName()).concat(".").concat(extension);
			}
			
			DLFileEntry newFileEntry=DLFileEntryLocalServiceUtil
				.addFileEntry(fileEntry.getUserId(), destinationGroupId, newParentFolderId,
				    name, null, fileEntry.getDescription(),
				    null, fileEntry.getExtraSettings(), is,
				    fileEntry.getSize(), serviceContext);
			
			log.info(">>>>>>> create newdocument "+
				newFileEntry.getTitle()+" | "+newFileEntry.toString());
			System.out.println(">>>>>>> create newdocument "+
				newFileEntry.getTitle()+" | "+newFileEntry.toString());
			
			movePermission(DLFileEntry.class.getName(),fileEntry.getCompanyId(),fileEntry.getGroupId(),
				fileEntry.getFileEntryId(),newFileEntry.getCompanyId(),newFileEntry.getGroupId(),
				newFileEntry.getFileEntryId());
		}
	}
	
	private void movePermission(String name, long oldCompanyId, long oldGroupId, long oldPrimKey,
			long newCompanyId, long newGroupId, long newPrimKey) throws Exception{
		List<ResourcePermission> rps=new ArrayList<ResourcePermission>();
		Group guestGroup=GroupLocalServiceUtil.getGroup(
				oldCompanyId, GroupConstants.GUEST);
		Group newGuestGroup=GroupLocalServiceUtil.getGroup(
				newCompanyId, GroupConstants.GUEST);
		
		//company
		List<ResourcePermission> crp=ResourcePermissionLocalServiceUtil
			.getResourcePermissions(oldCompanyId,
				name, ResourceConstants.SCOPE_COMPANY,
				String.valueOf(oldPrimKey));
		if(crp!=null && crp.size()>0){
			rps.addAll(crp);
		}
		
		//guest
		List<ResourcePermission> grp=ResourcePermissionLocalServiceUtil
			.getResourcePermissions(oldCompanyId,
				name, ResourceConstants.SCOPE_GROUP,
				String.valueOf(guestGroup.getGroupId()));
		if(grp!=null && grp.size()>0){
			rps.addAll(grp);
		}
		
		//group
		List<ResourcePermission> grrp=ResourcePermissionLocalServiceUtil
			.getResourcePermissions(oldCompanyId,
				name, ResourceConstants.SCOPE_GROUP,
				String.valueOf(oldGroupId));
		if(grrp!=null && grrp.size()>0){
			rps.addAll(grrp);
		}
		
		//individual
		List<ResourcePermission> irp=ResourcePermissionLocalServiceUtil
			.getResourcePermissions(oldCompanyId,
				name, ResourceConstants.SCOPE_INDIVIDUAL,
				String.valueOf(oldPrimKey));
		if(irp!=null && irp.size()>0){
			rps.addAll(irp);
		}
		
		for(ResourcePermission rp:rps){
			//actionIds with role owner created automatically when create folder
			//so no need to create one
			Role ownerRole = RoleLocalServiceUtil.getRole(newCompanyId, RoleConstants.OWNER);
			if(rp.getRoleId()!=ownerRole.getRoleId()){
				long rpId=CounterLocalServiceUtil.increment(ResourcePermission.class.getName());
				ResourcePermission nrp=ResourcePermissionLocalServiceUtil
					.createResourcePermission(rpId);
				nrp.setCompanyId(newCompanyId);
				nrp.setName(rp.getName());
				nrp.setScope(rp.getScope());
				nrp.setRoleId(rp.getRoleId());
				nrp.setActionIds(rp.getActionIds());
				if(rp.getScope()==ResourceConstants.SCOPE_GROUP){
					if(guestGroup!=null && rp.getPrimKey().equals(guestGroup.getGroupId())
							&& newGuestGroup!=null){
						//guest
						nrp.setPrimKey(String.valueOf(newGuestGroup.getGroupId()));
					}else{
						//group
						nrp.setPrimKey(String.valueOf(newGroupId));
					}
				}else{
					nrp.setPrimKey(String.valueOf(newPrimKey));
				}
				
				ResourcePermissionLocalServiceUtil.updateResourcePermission(nrp);
				ResourcePermissionLocalServiceUtil.deleteResourcePermission(rp);
			}
		}
	}
	
}
