package com.eclipselink.analyzer;

import org.junit.Test;
import org.junit.Assert;
import org.eclipse.persistence.sessions.Session;
import org.eclipse.persistence.sessions.server.ServerSession;
import org.eclipse.persistence.descriptors.ClassDescriptor;

import com.eclipselink.analyzer.MetamodelExtractor;
import com.eclipselink.analyzer.model.EntityNode;

import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Unit tests for Cache Configuration extraction.
 */
public class CacheExtractionTest {

    @Test
    public void testFullCacheTypeExtraction() {
        Session session = mock(Session.class);
        ClassDescriptor descriptor = mock(ClassDescriptor.class);

        // Mock FULL cache configuration
        when(descriptor.getIdentityMapClass())
                .thenReturn(org.eclipse.persistence.internal.identitymaps.FullIdentityMap.class);
        when(descriptor.getIdentityMapSize()).thenReturn(500);
        when(descriptor.getCacheInvalidationPolicy()).thenReturn(null);
        when(descriptor.getCacheSynchronizationType()).thenReturn(ClassDescriptor.SEND_OBJECT_CHANGES);
        when(descriptor.getCacheIsolation()).thenReturn(org.eclipse.persistence.config.CacheIsolationType.SHARED);
        when(descriptor.shouldAlwaysRefreshCache()).thenReturn(false);
        when(descriptor.shouldOnlyRefreshCacheIfNewerVersion()).thenReturn(false);
        when(descriptor.shouldDisableCacheHits()).thenReturn(false);

        when(session.getDescriptors()).thenReturn(java.util.Collections.singletonMap(Object.class, descriptor));

        MetamodelExtractor extractor = new MetamodelExtractor();
        List<EntityNode> nodes = extractor.extract(session);

        Assert.assertFalse("Nodes list should not be empty", nodes.isEmpty());
        EntityNode node = nodes.get(0);

        Assert.assertEquals("Cache type should be FULL", "FULL", node.getCacheType());
        Assert.assertEquals("Cache size should be 500", Integer.valueOf(500), node.getCacheSize());
        Assert.assertEquals("Cache isolation should be SHARED", "SHARED", node.getCacheIsolation());
        Assert.assertFalse("Always refresh should be false", node.getCacheAlwaysRefresh());
        Assert.assertFalse("Refresh if newer should be false", node.getCacheRefreshOnlyIfNewer());
        Assert.assertFalse("Disable hits should be false", node.getCacheDisableHits());
    }

    @Test
    public void testWeakCacheTypeExtraction() {
        Session session = mock(Session.class);
        ClassDescriptor descriptor = mock(ClassDescriptor.class);

        // Mock WEAK cache configuration
        when(descriptor.getIdentityMapClass())
                .thenReturn(org.eclipse.persistence.internal.identitymaps.WeakIdentityMap.class);
        when(descriptor.getIdentityMapSize()).thenReturn(100);
        when(descriptor.getCacheInvalidationPolicy()).thenReturn(null);
        when(descriptor.getCacheSynchronizationType()).thenReturn(ClassDescriptor.INVALIDATE_CHANGED_OBJECTS);
        when(descriptor.getCacheIsolation()).thenReturn(null);
        when(descriptor.shouldAlwaysRefreshCache()).thenReturn(false);
        when(descriptor.shouldOnlyRefreshCacheIfNewerVersion()).thenReturn(false);
        when(descriptor.shouldDisableCacheHits()).thenReturn(false);

        when(session.getDescriptors()).thenReturn(java.util.Collections.singletonMap(Object.class, descriptor));

        MetamodelExtractor extractor = new MetamodelExtractor();
        List<EntityNode> nodes = extractor.extract(session);

        EntityNode node = nodes.get(0);

        Assert.assertEquals("Cache type should be WEAK", "WEAK", node.getCacheType());
        Assert.assertEquals("Cache size should be 100", Integer.valueOf(100), node.getCacheSize());
        Assert.assertNull("Cache isolation should be null", node.getCacheIsolation());
    }

    @Test
    public void testSoftWeakCacheTypeExtraction() {
        Session session = mock(Session.class);
        ClassDescriptor descriptor = mock(ClassDescriptor.class);

        // Mock SOFT_WEAK cache configuration (default EclipseLink)
        when(descriptor.getIdentityMapClass())
                .thenReturn(org.eclipse.persistence.internal.identitymaps.SoftCacheWeakIdentityMap.class);
        when(descriptor.getIdentityMapSize()).thenReturn(100);
        when(descriptor.getCacheInvalidationPolicy()).thenReturn(null);
        when(descriptor.getCacheSynchronizationType()).thenReturn(ClassDescriptor.SEND_NEW_OBJECTS_WITH_CHANGES);
        when(descriptor.getCacheIsolation()).thenReturn(null);
        when(descriptor.shouldAlwaysRefreshCache()).thenReturn(false);
        when(descriptor.shouldOnlyRefreshCacheIfNewerVersion()).thenReturn(false);
        when(descriptor.shouldDisableCacheHits()).thenReturn(false);

        when(session.getDescriptors()).thenReturn(java.util.Collections.singletonMap(Object.class, descriptor));

        MetamodelExtractor extractor = new MetamodelExtractor();
        List<EntityNode> nodes = extractor.extract(session);

        EntityNode node = nodes.get(0);

        Assert.assertEquals("Cache type should be SOFT_WEAK", "SOFT_WEAK", node.getCacheType());
        Assert.assertEquals("Cache size should be 100", Integer.valueOf(100), node.getCacheSize());
        Assert.assertNull("Cache isolation should be null", node.getCacheIsolation());
    }

    @Test
    public void testCacheExpiryExtraction() {
        Session session = mock(Session.class);
        ClassDescriptor descriptor = mock(ClassDescriptor.class);

        // Mock cache with expiry
        when(descriptor.getIdentityMapClass())
                .thenReturn(org.eclipse.persistence.internal.identitymaps.SoftIdentityMap.class);
        when(descriptor.getIdentityMapSize()).thenReturn(100);

        org.eclipse.persistence.descriptors.invalidation.TimeToLiveCacheInvalidationPolicy invalidationPolicy = mock(
                org.eclipse.persistence.descriptors.invalidation.TimeToLiveCacheInvalidationPolicy.class);
        when(invalidationPolicy.getTimeToLive()).thenReturn(3600000L); // 1 hour

        when(descriptor.getCacheInvalidationPolicy()).thenReturn(invalidationPolicy);

        when(session.getDescriptors()).thenReturn(java.util.Collections.singletonMap(Object.class, descriptor));

        MetamodelExtractor extractor = new MetamodelExtractor();
        List<EntityNode> nodes = extractor.extract(session);

        EntityNode node = nodes.get(0);

        Assert.assertEquals("Cache type should be SOFT", "SOFT", node.getCacheType());
        Assert.assertEquals("Cache size should be 100", Integer.valueOf(100), node.getCacheSize());
        Assert.assertEquals("Cache expiry should be 3600000ms (1h)", Integer.valueOf(3600000), node.getCacheExpiry());
    }

    @Test
    public void testCacheCoordinationTypeExtraction() {
        Session session = mock(Session.class);
        ClassDescriptor descriptor = mock(ClassDescriptor.class);

        // Mock INVALIDATE_CHANGED_OBJECTS coordination
        when(descriptor.getIdentityMapClass())
                .thenReturn(org.eclipse.persistence.internal.identitymaps.SoftIdentityMap.class);
        when(descriptor.getIdentityMapSize()).thenReturn(100);
        when(descriptor.getCacheInvalidationPolicy()).thenReturn(null);
        when(descriptor.getCacheSynchronizationType()).thenReturn(ClassDescriptor.INVALIDATE_CHANGED_OBJECTS);
        when(descriptor.getCacheIsolation()).thenReturn(null);
        when(descriptor.shouldAlwaysRefreshCache()).thenReturn(false);
        when(descriptor.shouldOnlyRefreshCacheIfNewerVersion()).thenReturn(false);
        when(descriptor.shouldDisableCacheHits()).thenReturn(false);

        when(session.getDescriptors()).thenReturn(java.util.Collections.singletonMap(Object.class, descriptor));

        MetamodelExtractor extractor = new MetamodelExtractor();
        List<EntityNode> nodes = extractor.extract(session);

        EntityNode node = nodes.get(0);

        Assert.assertEquals("Cache type should be SOFT", "SOFT", node.getCacheType());
        Assert.assertEquals("Cache coordination should be INVALIDATE_CHANGED_OBJECTS",
                "INVALIDATE_CHANGED_OBJECTS", node.getCacheCoordinationType());
    }

    @Test
    public void testCacheIsolationExtraction() {
        Session session = mock(Session.class);
        ClassDescriptor descriptor = mock(ClassDescriptor.class);

        // Mock ISOLATED cache
        when(descriptor.getIdentityMapClass())
                .thenReturn(org.eclipse.persistence.internal.identitymaps.NoIdentityMap.class);
        when(descriptor.getIdentityMapSize()).thenReturn(100);
        when(descriptor.getCacheInvalidationPolicy()).thenReturn(null);
        when(descriptor.getCacheSynchronizationType()).thenReturn(ClassDescriptor.SEND_OBJECT_CHANGES);
        when(descriptor.getCacheIsolation()).thenReturn(org.eclipse.persistence.config.CacheIsolationType.ISOLATED);
        when(descriptor.shouldAlwaysRefreshCache()).thenReturn(false);
        when(descriptor.shouldOnlyRefreshCacheIfNewerVersion()).thenReturn(false);
        when(descriptor.shouldDisableCacheHits()).thenReturn(false);

        when(session.getDescriptors()).thenReturn(java.util.Collections.singletonMap(Object.class, descriptor));

        MetamodelExtractor extractor = new MetamodelExtractor();
        List<EntityNode> nodes = extractor.extract(session);

        EntityNode node = nodes.get(0);

        Assert.assertEquals("Cache type should be NONE", "NONE", node.getCacheType());
        Assert.assertEquals("Cache isolation should be ISOLATED", "ISOLATED", node.getCacheIsolation());
    }

    @Test
    public void testCacheAlwaysRefreshExtraction() {
        Session session = mock(Session.class);
        ClassDescriptor descriptor = mock(ClassDescriptor.class);

        // Mock cache with alwaysRefresh
        when(descriptor.getIdentityMapClass())
                .thenReturn(org.eclipse.persistence.internal.identitymaps.SoftIdentityMap.class);
        when(descriptor.getIdentityMapSize()).thenReturn(100);
        when(descriptor.getCacheInvalidationPolicy()).thenReturn(null);
        when(descriptor.getCacheSynchronizationType()).thenReturn(ClassDescriptor.SEND_OBJECT_CHANGES);
        when(descriptor.getCacheIsolation()).thenReturn(null);
        when(descriptor.shouldAlwaysRefreshCache()).thenReturn(true);
        when(descriptor.shouldOnlyRefreshCacheIfNewerVersion()).thenReturn(false);
        when(descriptor.shouldDisableCacheHits()).thenReturn(false);

        when(session.getDescriptors()).thenReturn(java.util.Collections.singletonMap(Object.class, descriptor));

        MetamodelExtractor extractor = new MetamodelExtractor();
        List<EntityNode> nodes = extractor.extract(session);

        EntityNode node = nodes.get(0);

        Assert.assertEquals("Cache type should be SOFT", "SOFT", node.getCacheType());
        Assert.assertTrue("Always refresh should be true", node.getCacheAlwaysRefresh());
    }

    @Test
    public void testCacheDisableHitsExtraction() {
        Session session = mock(Session.class);
        ClassDescriptor descriptor = mock(ClassDescriptor.class);

        // Mock cache with disableHits
        when(descriptor.getIdentityMapClass())
                .thenReturn(org.eclipse.persistence.internal.identitymaps.SoftIdentityMap.class);
        when(descriptor.getIdentityMapSize()).thenReturn(100);
        when(descriptor.getCacheInvalidationPolicy()).thenReturn(null);
        when(descriptor.getCacheSynchronizationType()).thenReturn(ClassDescriptor.SEND_OBJECT_CHANGES);
        when(descriptor.getCacheIsolation()).thenReturn(null);
        when(descriptor.shouldAlwaysRefreshCache()).thenReturn(false);
        when(descriptor.shouldOnlyRefreshCacheIfNewerVersion()).thenReturn(false);
        when(descriptor.shouldDisableCacheHits()).thenReturn(true);

        when(session.getDescriptors()).thenReturn(java.util.Collections.singletonMap(Object.class, descriptor));

        MetamodelExtractor extractor = new MetamodelExtractor();
        List<EntityNode> nodes = extractor.extract(session);

        EntityNode node = nodes.get(0);

        Assert.assertEquals("Cache type should be SOFT", "SOFT", node.getCacheType());
        Assert.assertTrue("Disable hits should be true", node.getCacheDisableHits());
    }

    @Test
    public void testCacheRefreshOnlyIfNewerExtraction() {
        Session session = mock(Session.class);
        ClassDescriptor descriptor = mock(ClassDescriptor.class);

        // Mock cache with refreshOnlyIfNewer
        when(descriptor.getIdentityMapClass())
                .thenReturn(org.eclipse.persistence.internal.identitymaps.SoftIdentityMap.class);
        when(descriptor.getIdentityMapSize()).thenReturn(100);
        when(descriptor.getCacheInvalidationPolicy()).thenReturn(null);
        when(descriptor.getCacheSynchronizationType()).thenReturn(ClassDescriptor.SEND_OBJECT_CHANGES);
        when(descriptor.getCacheIsolation()).thenReturn(null);
        when(descriptor.shouldAlwaysRefreshCache()).thenReturn(false);
        when(descriptor.shouldOnlyRefreshCacheIfNewerVersion()).thenReturn(true);
        when(descriptor.shouldDisableCacheHits()).thenReturn(false);

        when(session.getDescriptors()).thenReturn(java.util.Collections.singletonMap(Object.class, descriptor));

        MetamodelExtractor extractor = new MetamodelExtractor();
        List<EntityNode> nodes = extractor.extract(session);

        EntityNode node = nodes.get(0);

        Assert.assertEquals("Cache type should be SOFT", "SOFT", node.getCacheType());
        Assert.assertTrue("Refresh if newer should be true", node.getCacheRefreshOnlyIfNewer());
    }
}
