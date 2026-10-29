package name.yumao.ffxiv.chn.model;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import name.yumao.ffxiv.chn.util.FFCRC;
import name.yumao.ffxiv.chn.util.LERandomAccessFile;

/**
 * 同步改寫 .index2 的檔案位址。
 * .index2 以「完整路徑」的 CRC 作為 key，每筆 8 bytes：hash(4) + offset(4)。
 * 若 .index2 不存在則什麼都不做。
 */
public class SqPackIndex2Patcher implements Closeable {

    private final LERandomAccessFile index2File;
    private final HashMap<Integer, SqPackIndexFile> files;

    /**
     * @param indexPath .index 的路徑，.index2 為其後加上 "2"
     */
    public SqPackIndex2Patcher(String indexPath) throws Exception {
        File file = new File(indexPath + "2");
        if (!indexPath.endsWith("index") || !file.isFile()) {
            this.index2File = null;
            this.files = null;
            System.out.println("Index2 not found, skip : " + file.getAbsolutePath());
            return;
        }
        HashMap<Integer, SqPackIndexFolder> index2 = new SqPackIndex(file.getAbsolutePath()).resloveIndex();
        SqPackIndexFolder folder = index2.get(0);
        this.files = folder == null ? null : folder.getFiles();
        this.index2File = new LERandomAccessFile(file, "rw");
    }

    /**
     * @param fullPath   完整路徑，例如 exd/item_0_ja.exd
     * @param offsetDiv8 新的資料位址（dat 內位移 / 8）
     * @return 是否有改到
     */
    public boolean update(String fullPath, int offsetDiv8) throws IOException {
        if (index2File == null || files == null) {
            return false;
        }
        int hash = FFCRC.ComputeCRC(fullPath.toLowerCase().getBytes());
        SqPackIndexFile entry = files.get(hash);
        if (entry == null) {
            System.out.println("Index2 Miss : " + fullPath);
            return false;
        }
        // 每筆：hash(4) + offset(4)
        index2File.seek(entry.getPt() + 4);
        index2File.writeInt(offsetDiv8);
        return true;
    }

    @Override
    public void close() throws IOException {
        if (index2File != null) {
            index2File.close();
        }
    }
}
