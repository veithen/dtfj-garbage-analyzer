import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.ibm.dtfj.image.Image;
import com.ibm.dtfj.image.ImageAddressSpace;
import com.ibm.dtfj.image.j9.ImageFactory;
import com.ibm.dtfj.java.JavaClass;
import com.ibm.dtfj.java.JavaHeap;
import com.ibm.dtfj.java.JavaObject;
import com.ibm.dtfj.java.JavaReference;
import com.ibm.dtfj.java.JavaRuntime;
import com.ibm.dtfj.java.JavaStackFrame;
import com.ibm.dtfj.java.JavaThread;

public class GarbageAnalyzer {
    public static void main(String[] args) throws Exception {
        String dump = args[0];
        Image image = new ImageFactory().getImage(new File(dump), new File(dump + ".xml"));
        JavaRuntime runtime = null;
        loop: for (Iterator it = image.getAddressSpaces(); it.hasNext(); ) {
            ImageAddressSpace addressSpace = (ImageAddressSpace)it.next();
            for (Iterator it2 = addressSpace.getCurrentProcess().getRuntimes(); it2.hasNext(); ) {
                runtime = (JavaRuntime)it2.next();
                break loop;
            }
        }
        System.out.println("Starting mark");
        MarkSet marked = new MarkSet(true);
        for (Iterator it = runtime.getHeapRoots(); it.hasNext(); ) {
            marked.mark((JavaReference)it.next());
        }
        for (Iterator it = runtime.getThreads(); it.hasNext(); ) {
            for (Iterator it2 = ((JavaThread)it.next()).getStackFrames(); it2.hasNext(); ) {
                for (Iterator it3 = ((JavaStackFrame)it2.next()).getHeapRoots(); it3.hasNext(); ) {
                    marked.mark((JavaReference)it3.next());
                }
            }
        }
        System.out.println("Marked " + marked.getCount() + " objects");
        int dead = 0;
        Map<JavaClass,PerClassStats> statsMap = new HashMap<JavaClass,PerClassStats>();
        for (Iterator it = runtime.getHeaps(); it.hasNext(); ) {
            JavaHeap heap = (JavaHeap)it.next();
            for (Iterator it2 = heap.getObjects(); it2.hasNext(); ) {
                JavaObject object = (JavaObject)it2.next();
                if (!marked.isMarked(object)) {
                    dead++;
                    JavaClass clazz = object.getJavaClass();
                    PerClassStats stats = statsMap.get(clazz);
                    if (stats == null) {
                        stats = new PerClassStats();
                        statsMap.put(clazz, stats);
                    }
                    stats.instanceCount++;
                    stats.totalSize += object.getSize();
                }
            }
        }
        System.out.println("Found " + dead + " dead objects");
        for (Map.Entry<JavaClass,PerClassStats> entry : statsMap.entrySet()) {
            PerClassStats stats = entry.getValue();
            System.out.println(entry.getKey().getName() + " " + stats.instanceCount + " " + stats.totalSize);
        }
    }
}
