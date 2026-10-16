package neelesh.easy_install;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class InstallationState {

    private ConcurrentHashMap<String, ProjectStatus> projectState = new ConcurrentHashMap<>();

    private ConcurrentHashMap.KeySetView<String, Boolean> installingHashes = ConcurrentHashMap.newKeySet();


    public ProjectStatus getProjectStatus(String id) {
        ProjectStatus projectStatus = projectState.get(id);
        if (projectStatus == null) return ProjectStatus.NONE;
        return projectStatus;
    }

    public boolean isInstalled(String id) {
        return projectState.get(id) == ProjectStatus.INSTALLED || projectState.get(id) == ProjectStatus.NEEDS_UPDATE;
    }

    public void addInstallingHash(String hash) {
        installingHashes.add(hash);
    }

    public void removeInstallingHash(String hash) {
        installingHashes.remove(hash);
    }

    public boolean isInstallingHash(String hash) {
        return installingHashes.contains(hash);
    }

    public int getNumUpdatesInstalling() {
        int count = 0;
        for (ProjectStatus status : projectState.values()) {
            if (status.equals(ProjectStatus.INSTALLING_UPDATE)) {
                count++;
            }
        }
        return count;
    }

    public boolean needsUpdate(String id) {
        return projectState.get(id) == ProjectStatus.NEEDS_UPDATE
                || projectState.get(id) == ProjectStatus.INSTALLING_UPDATE;
    }

    public boolean isInstalling(String id) {
        return projectState.get(id) == ProjectStatus.INSTALLING_REGULAR
                || projectState.get(id) == ProjectStatus.INSTALLING_UPDATE;
    }

    public void setProjectStatus(String id, ProjectStatus projectStatus) {
        if (projectStatus.equals(ProjectStatus.NONE)) {
            projectState.remove(id);
        } else {
            projectState.put(id, projectStatus);
        }
    }
    public void loadInstallationState(HashSet<String> installedProjects, HashSet<String> updatesNeeded) {
        ConcurrentHashMap<String, ProjectStatus> tempProjectState = new ConcurrentHashMap<>(projectState);
        tempProjectState.entrySet().removeIf(e -> !e.getValue().equals(ProjectStatus.INSTALLING_REGULAR)
                                           && !e.getValue().equals(ProjectStatus.INSTALLING_UPDATE));
        for (String s : installedProjects) {
            tempProjectState.put(s, ProjectStatus.INSTALLED);
        }
        for (String s : updatesNeeded) {
            if (tempProjectState.get(s) != ProjectStatus.INSTALLING_UPDATE) {
                tempProjectState.put(s, ProjectStatus.NEEDS_UPDATE);
            }
        }
        projectState = tempProjectState;
    }

    public void removeProject(String id) {
        projectState.remove(id);
    }

    public void clear() {
        projectState.clear();
    }





}
