package com.storyteller_f.file_system.instance.local;

import android.content.Context;

import com.storyteller_f.file_system.instance.BaseContextFileInstance;

/**
 * 定义接口，方法
 */
@SuppressWarnings("ALL")
public abstract class LocalFileInstance extends BaseContextFileInstance {

    private static final String TAG = "FileInstance";

    public LocalFileInstance(Context context, String path, String fileSystemRoot) {
        super(context, path, fileSystemRoot);
    }

//    @NonNull
//    @WorkerThread
//    public FilesAndDirectories listSafe() {
//        ArrayList<FileItemModel> files = new ArrayList<>();
//        ArrayList<DirectoryItemModel> directories = new ArrayList<>();
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            try {
//                return getFilesAndDirectoriesOnWalk(files, directories);
//            } catch (Exception e) {
//                e.printStackTrace();
//                return list();
//            }
//        } else return list();
//    }
//
//    @RequiresApi(api = Build.VERSION_CODES.O)
//    @WorkerThread
//    private FilesAndDirectories getFilesAndDirectoriesOnWalk(final ArrayList<FileItemModel> files, final ArrayList<DirectoryItemModel> directories) throws IOException {
//        FileVisitor<Path> visitor = builderVisitor(files, directories);
//        Files.walkFileTree(new File(path).toPath(), EnumSet.of(FileVisitOption.FOLLOW_LINKS), 1, visitor);
//        return new FilesAndDirectories(files, directories);
//    }
//
//    @NonNull
//    private FileVisitor<Path> builderVisitor(ArrayList<FileItemModel> files, ArrayList<DirectoryItemModel> directories) {
//        FileVisitor<Path> visitor = new FileVisitor<Path>() {
//            @Override
//            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
//                if (needStop()) return FileVisitResult.TERMINATE;
//                return FileVisitResult.CONTINUE;
//            }
//
//            @Override
//            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
//                if (needStop()) return FileVisitResult.TERMINATE;
//                File childFile = file.toFile();
//                save(childFile, files, directories);
//                return FileVisitResult.CONTINUE;
//            }
//
//            @Override
//            public FileVisitResult visitFileFailed(Path file, IOException exc) {
//                if (needStop()) return FileVisitResult.TERMINATE;
//                return FileVisitResult.CONTINUE;
//            }
//
//            @Override
//            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
//                if (needStop()) return FileVisitResult.TERMINATE;
//                return FileVisitResult.CONTINUE;
//            }
//        };
//        return visitor;
//    }
//
//    private void save(File childFile, ArrayList<FileItemModel> files, ArrayList<DirectoryItemModel> directories) {
//        FileSystemItemModel fileSystemItemModel;
//        boolean w = childFile.canWrite();
//        boolean r = childFile.canRead();
//        boolean x = childFile.canExecute();
//        String detail = String.format(Locale.CHINA, "%c%c%c%c", (childFile.isFile() ? '-' : 'd'), (r ? 'r' : '-'), (w ? 'w' : '-'), (x ? 'e' : '-'));
//        if (childFile.isFile()) {
//            fileSystemItemModel = addFile(files, path, childFile, detail);
//        } else {
//            fileSystemItemModel = addDirectory(directories, path, childFile, detail);
//        }
//        editAccessTime(childFile, fileSystemItemModel);
//    }

}
