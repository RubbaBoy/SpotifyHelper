package com.uddernetworks.spotifyhelper;

import static android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class SpotifyAccessibilityService extends AccessibilityService {

    @Override
    protected void onServiceConnected() {
        var info = getServiceInfo();

        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.flags = AccessibilityServiceInfo.DEFAULT;
        info.packageNames = new String[] {"com.spotify.music"};

        super.setServiceInfo(info);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_LONG_CLICKED) {
            var source = event.getSource();
            if (source == null) {
                return;
            }

            // Then it's probably not it
            if (!matchesTree(source, "android.view.ViewGroup", "androidx.recyclerview.widget.RecyclerView", "android.view.ViewGroup", "android.widget.FrameLayout")) {
                return;
            }

            // Perform 60 checks (3 seconds) or else it's probably not gonna happen
            var count = new AtomicInteger(60);
            CompletableFuture.runAsync(() -> {
                while (true) {
                    var root = getRootInActiveWindow();
                    var queues = root.findAccessibilityNodeInfosByText("Add to Queue");

                    if (queues.isEmpty()) {
                        if (count.decrementAndGet() <= 0) {
                            break;
                        }

                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException ignored) {
                        }
                        continue;
                    }

                    var queue = queues.get(0);
                    var parent = queue.getParent(); // LinearLayour
                    if (queue.isClickable() && parent.isClickable()) {
                        queue.performAction(ACTION_CLICK);
                        parent.performAction(ACTION_CLICK);
                    }
                    break;
                }
            });
        }
    }

    /**
     * Checks if the given info matches the given list, top to lowest level.
     * For example, if you were matching a ViewGroup (as info) with a parent of FrameLayour, the
     * parents would be
     * <code>android.view.ViewGroup, android.widget.FrameLayout</code>
     * @param info The node
     * @param parents The parents' class paths, from highest to lowest level
     * @return If it matches
     */
    private boolean matchesTree(AccessibilityNodeInfo info, String... parents) {
        return matchesTree(info, Arrays.asList(parents));
    }

    /**
     * Checks if the given info matches the given list, top to lowest level.
     * For example, if you were matching a ViewGroup (as info) with a parent of FrameLayour, the
     * parents would be
     * <code>android.view.ViewGroup, android.widget.FrameLayout</code>
     * @param info The node
     * @param parents The parents' class paths, from highest to lowest level
     * @return If it matches
     */
    private boolean matchesTree(AccessibilityNodeInfo info, List<String> parents) {
        if (info == null) {
            return parents.isEmpty();
        }

        if (!info.getClassName().equals(parents.get(0))) {
            return false;
        }

        return matchesTree(info.getParent(), parents.subList(1, parents.size()));
    }

    @Override
    public void onInterrupt() {

    }
}
