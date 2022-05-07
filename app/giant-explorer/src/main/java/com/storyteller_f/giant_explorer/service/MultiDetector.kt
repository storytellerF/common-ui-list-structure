package com.storyteller_f.giant_explorer.service;

import android.content.Context;

import com.storyteller_f.file_system.FileInstanceFactory;
import com.storyteller_f.file_system.instance.FileInstance;
import com.storyteller_f.file_system.model.FileItemModel;
import com.storyteller_f.file_system.model.FileSystemItemModel;
import com.storyteller_f.file_system.model.FilesAndDirectories;

import java.util.LinkedList;
import java.util.List;


public class MultiDetector {
    private final List<FileSystemItemModel> selected;

    public MultiDetector(List<FileSystemItemModel> selected) {
        this.selected = selected;
    }


    public LinkedList<DetectorTask> start(Context context) {
        LinkedList<DetectorTask> detectorTasks;
        detectorTasks = new LinkedList<>();
        for (FileSystemItemModel fim : selected) {
            FileInstance fileInstance = FileInstanceFactory.getFileInstance(fim.getFullPath(), context);
            if (!fileInstance.exists()) {
                detectorTasks.add(new ErrorTask(fim,fim.getName() + "文件不存在"));
                return detectorTasks;
            }
            if (fim instanceof FileItemModel) {
                detectorTasks.add(new ValidTask(fim, ValidTask.type_file));
            } else {
                FileInstance instance = FileInstanceFactory.getFileInstance(fim.getFullPath(), context);
                //检查文件下的文件个数
                FilesAndDirectories list = instance.listSafe();
                if (list == null) {
                    detectorTasks.add(new ErrorTask(fim,fim.getName() + "listFiles() 失败"));
                    return detectorTasks;
                }
                if (list.getCount() == 0) {
                    detectorTasks.add(new ValidTask(fim, ValidTask.type_empty));
                } else {
                    detectorTasks.add(new ValidTask(fim, ValidTask.type_not_empty));
                }
            }
        }
        return detectorTasks;
    }
}
