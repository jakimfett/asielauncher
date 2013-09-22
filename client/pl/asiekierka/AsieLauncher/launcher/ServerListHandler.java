package pl.asiekierka.AsieLauncher.launcher;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import org.jnbt.*;

public class ServerListHandler {
	private String filename;
	private File file;
	private boolean hideAddress;
	
	public String getFilename() {
		return filename;
	}

	public ServerListHandler(String _filename, boolean _hideAddress) {
		filename = _filename;
		hideAddress = _hideAddress;
		file = new File(filename);
	}
	
	private boolean isSame(String name, String ip, CompoundTag tag) {
		boolean isEqual = false;
		for(Entry<String, Tag> entry: tag.getValue().entrySet()) {
			if(!(entry.getValue() instanceof StringTag)) continue;
			String value = ((StringTag)(entry.getValue())).getValue();
			if(entry.getKey().equals("name")) {
				isEqual = isEqual || (value.equals(name));
			} else if(entry.getKey().equals("ip")) {
				isEqual = isEqual || (value.equals(ip));
			}
		}
		return isEqual;
	}
	
	private CompoundTag addServer(String name, String ip) {
		Map<String, Tag> serverMap = new HashMap<String, Tag>();
		serverMap.put("name", (Tag)new StringTag("name", name));
		serverMap.put("ip", (Tag)new StringTag("ip", ip));
		serverMap.put("hideAddress", (Tag)new ByteTag("hideAddress", hideAddress?(byte)1:(byte)0));
		return new CompoundTag("", serverMap);
	}
	private List<Tag> mapToList(Map<String, String> servers) {
		ArrayList<Tag> serverList = new ArrayList<Tag>();
		for(Entry<String, String> entry: servers.entrySet()) {
			serverList.add((Tag)addServer(entry.getKey(), entry.getValue()));
		}
		return serverList;
	}
	public void updateServerList(Map<String, String> servers) {
		NBTInputStream nbt = null;
		Tag tag = null;
		ListTag serverList = null;
		try {
			nbt = new NBTInputStream(new FileInputStream(file), false);
			tag = nbt.readTag();
			nbt.close();
		}
		catch(Exception e) { try { if(nbt != null) nbt.close(); } catch(Exception ee) {} }
		if(!(tag instanceof CompoundTag)) tag = null;
		if(tag != null) {
			// Read initial serverList.
			CompoundTag ctag = (CompoundTag)tag;
			serverList = (ListTag)ctag.getTag("servers");
			// Create new serverlist.
			Map<String, String> serversToAdd = new HashMap<String, String>();
			serversToAdd.putAll(servers);
			ArrayList<Tag> serverArray = new ArrayList<Tag>();
			// Iterate over every server in the list.
			for(Tag stag: serverList.getValue()) {
				if(!(stag instanceof CompoundTag)) continue;
				CompoundTag sctag = (CompoundTag)stag;
				boolean foundServer = false;
				String removeKey = null;
				// Iterate over all of our servers.
				for(Entry<String, String> e: servers.entrySet()) {
					if(isSame(e.getKey(), e.getValue(), sctag)) {
						foundServer = true;
						removeKey = e.getKey();
						// Check if we haven't added that server already.
						if(serversToAdd.containsKey(e.getKey())) { 
							serverArray.add((Tag)addServer(e.getKey(), e.getValue()));
						}
					}
				}
				// If it's not our server, keep it.
				if(!foundServer) serverArray.add(stag);
				else serversToAdd.remove(removeKey);
			}
			// Add remaining servers.
			for(Entry<String, String> e: serversToAdd.entrySet()) {
				serverArray.add((Tag)addServer(e.getKey(), e.getValue()));
			}
			// Set serverList to the new list.
			serverList = new ListTag("servers", CompoundTag.class, serverArray);
			// Debug: List servers we have.
			System.out.println(serverList.toString());
		} else {
			// Generate a new serverList based on the list of servers from config.json.
			serverList = new ListTag("servers", CompoundTag.class, mapToList(servers));
		}
		// Save the serverlist.
		HashMap<String, Tag> outputMap = new HashMap<String, Tag>(1);
		outputMap.put("servers", serverList);
		tag = new CompoundTag("", outputMap);
		NBTOutputStream nbto = null;
		try {
			nbto = new NBTOutputStream(new FileOutputStream(file), false);
			nbto.writeTag(tag);
			nbto.close();
		}
		catch(Exception e) { try { if(nbto != null) nbto.close(); } catch(Exception ee) {} }
	}

}
