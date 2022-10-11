package com.storyteller_f.file_system;


import com.storyteller_f.file_system.model.DirectoryItemModel;
import com.storyteller_f.file_system.model.FileItemModel;

import java.util.List;

public interface Filter {
    /**
     * 当添加一个文件目录时
     *
     * @param parent       当前父目录
     * @param absolutePath 当前文件
     * @param isFile       是否时文件
     * @return 如果是true，将会添加
     */
    boolean onPath(String parent, String absolutePath, boolean isFile);

    /**
     * 所有索引过程结束，用户可以在这个方法下添加自己的文件
     *
     * @param parent 当前目录
     * @return 需要添加的文件，如果是null，将不会添加
     */
    List<FileItemModel> onFile(String parent);

    /**
     * 所有索引过程结束，用户可以在这个方法下添加自己的文件夹
     *
     * @param parent 当前目录
     * @return 需要添加的文件夹，如果是null，将不会添加
     */
    List<DirectoryItemModel> onDirectory(String parent);
}
