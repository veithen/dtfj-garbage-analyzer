import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

import com.ibm.dtfj.java.JavaClass;
import com.ibm.dtfj.java.JavaObject;
import com.ibm.dtfj.java.JavaReference;

public class MarkSet {
    private final Map<Long,byte[]> bitmaps = new HashMap<Long,byte[]>();
    
    /**
     * Stack of {@link JavaObject} and {@link JavaClass} instances that have been marked but the
     * references of which have not been processed yet. We indeed need to use a stack or queue here:
     * recursion would not work because the recursion depth may be too large and cause stack
     * overflows. Empirically, a stack works better than a queue (the size remains smaller).
     */
    private final Stack<Object> unprocessed = new Stack<Object>();
    
    private int count;
    
    private boolean getSetMark(long address, boolean set) {
        address /= 4; // Assume alignment is 32bit
        Long chunkIndex = address >> 18; // We use one bitmap per 2^18 addresses, i.e. per MB of heap
        byte[] chunk = bitmaps.get(chunkIndex);
        if (chunk == null) {
            if (!set) {
                return false;
            }
            chunk = new byte[1 << 15];
            bitmaps.put(chunkIndex, chunk);
        }
        int bitIndex = (int)(address & ((1 << 18) - 1));
        int arrayIndex = bitIndex / 8;
        int mask = 1 << (bitIndex % 8);
        byte b = chunk[arrayIndex];
        if ((b & mask) != 0) {
            // Already marked
            return true;
        } else {
            if (set) {
                b |= mask;
                chunk[arrayIndex] = b;
            }
            return false;
        }
    }
    
    public void mark(JavaObject object) throws Exception {
        markOne(object);
        process();
    }
    
    private void markOne(JavaObject object) throws Exception {
        if (!getSetMark(object.getID().getAddress(), true)) {
            unprocessed.add(object);
            count++;
            if (count % 1000 == 0) {
                System.out.println("Marked " + count + " objects (queue=" + unprocessed.size() + ")");
            }
        }
    }
    
    public void mark(JavaClass clazz) throws Exception {
        markOne(clazz);
        process();
    }
    
    private void markOne(JavaClass clazz) throws Exception {
        if (!getSetMark(clazz.getID().getAddress(), true)) {
            unprocessed.add(clazz);
        }
    }
    
    public void mark(JavaReference ref) throws Exception {
        markOne(ref);
        process();
    }

    private void markOne(JavaReference ref) throws Exception {
        Object target = ref.getTarget();
        if (target instanceof JavaClass) {
            markOne((JavaClass)target);
        } else {
            markOne((JavaObject)target);
        }
    }
    
    private void process() throws Exception {
        while (!unprocessed.isEmpty()) {
            Object object = unprocessed.pop();
            Iterator references;
            if (object instanceof JavaClass) {
                references = ((JavaClass)object).getReferences();
            } else {
                references = ((JavaObject)object).getReferences();
            }
            for (Iterator it = references; it.hasNext(); ) {
                markOne((JavaReference)it.next());
            }
        }
    }
    
    public boolean isMarked(JavaObject object) {
        return getSetMark(object.getID().getAddress(), false);
    }
    
    public int getCount() {
        return count;
    }
}
