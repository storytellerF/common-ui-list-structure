package com.storyteller_f.file_system.model;

import android.os.Build;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FilesAndDirectories {
    public static final int mode_name = 1;
    public static final int mode_torrent = 2;
    public static final int mode_size = 3;
    private List<FileItemModel> files;
    private List<DirectoryItemModel> directories;
    private AllInOneComparator allInOneComparator = new AllInOneComparator();
    private int sortMode = mode_name;
    private boolean ignoreCase;//忽略大小写

    public FilesAndDirectories(List<FileItemModel> files, List<DirectoryItemModel> directories) {
        this.files = files;
        this.directories = directories;
    }

    public int getSortMode() {
        return sortMode;
    }

    public void setSortMode(int sortMode) {
        this.sortMode = sortMode;
    }

    public boolean isIgnoreCase() {
        return ignoreCase;
    }

    public void setIgnoreCase(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    public List<FileItemModel> getFiles() {
        return files;
    }

    public List<DirectoryItemModel> getDirectories() {
        return directories;
    }

    public void addFiles(List<FileItemModel> fileItemModels) {
        files.addAll(fileItemModels);
    }

    public void sort(boolean reversed) {
        if (reversed) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                directories.sort(allInOneComparator.reversed());
                files.sort(allInOneComparator.reversed());
            } else {
                Collections.sort(directories, Collections.reverseOrder(allInOneComparator));
                Collections.sort(files, Collections.reverseOrder(allInOneComparator));
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                directories.sort(allInOneComparator);
                files.sort(allInOneComparator);
            } else {
                Collections.sort(directories, allInOneComparator);
                Collections.sort(files, allInOneComparator);
            }
        }
    }

    public void addDirectory(List<DirectoryItemModel> directoryItemModels) {
        directories.addAll(directoryItemModels);
    }

    public void destroy() {
        destroyFile();
        directories = null;
        allInOneComparator = null;
    }

    public boolean isSortByName() {
        return sortMode != mode_name;
    }

    public int getCount() {
        return files.size() + directories.size();
    }

    public void destroyFile() {
        files = null;
    }

    class AllInOneComparator implements Comparator<FileSystemItemModel> {

        @Override
        public int compare(FileSystemItemModel o1, FileSystemItemModel o2) {
            switch (sortMode) {
                case mode_torrent:
                    String name1;
                    String name2;
                    if (o1 instanceof TorrentFileModel) {
                        String torrentName = ((TorrentFileModel) o1).getTorrentName();
                        if (o1.getName().endsWith(".torrent")
                                && torrentName != null) {
                            name1 = torrentName;//种子文件的大小就是种子名
                        } else {
                            name1 = o1.getName();
                        }
                    } else {
                        name1 = o1.getName();
                    }
                    if (o2 instanceof TorrentFileModel) {
                        String torrentName = ((TorrentFileModel) o2).getTorrentName();
                        if (o2.getName().endsWith(".torrent")
                                && torrentName != null) {
                            name2 = torrentName;//种子文件的大小就是种子名
                        } else {
                            name2 = o2.getName();
                        }
                    } else {
                        name2 = o2.getName();
                    }


                    if (ignoreCase) return name1.compareToIgnoreCase(name2);
                    return name1.compareTo(name2);
                case mode_size:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        return Long.compare(o2.getSize(), o1.getSize());
                    } else {
                        //todo 版本兼容问题
                    }
                case mode_name:
                    if (ignoreCase) return o1.getName().compareToIgnoreCase(o2.getName());
                    else return o1.getName().compareTo(o2.getName());
                default:
                    return 0;
            }
        }
    }

}
