package org.elasticsearch.repository.ufile;

import org.elasticsearch.ucloud.ufile.blobstore.UfileBlobStore;
import org.elasticsearch.ucloud.ufile.service.UfileClientSettings;
import org.elasticsearch.ucloud.ufile.service.UfileService;
import org.elasticsearch.cluster.metadata.RepositoryMetaData;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.blobstore.BlobPath;
import org.elasticsearch.common.blobstore.BlobStore;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.env.Environment;
import org.elasticsearch.repositories.RepositoryException;
import org.elasticsearch.repositories.blobstore.BlobStoreRepository;

import java.io.File;
import java.io.IOException;

/**
 * An Ufile repository working for snapshot and restore.
 * Implementations are responsible for reading and writing both metadata and shard data to and from
 * a repository backend.
 * Created by yangkongshi on 2017/11/24.
 */
public class UfileRepository extends BlobStoreRepository {
    public static final String TYPE = "ufile";
    private final UfileBlobStore blobStore;
    private final BlobPath basePath;
    private final boolean compress;
    private final ByteSizeValue chunkSize;

    public UfileRepository(RepositoryMetaData metadata, Environment env,
        NamedXContentRegistry namedXContentRegistry, UfileService ufileService) {
        super(metadata, env.settings(), namedXContentRegistry);
        String bucket = getSetting(UfileClientSettings.BUCKET, metadata);
        String basePath = UfileClientSettings.BASE_PATH.get(metadata.settings());
        if (Strings.hasLength(basePath)) {
            BlobPath path = new BlobPath();
            for (String elem : basePath.split(File.separator)) {
                path = path.add(elem);
            }
            this.basePath = path;
        } else {
            this.basePath = BlobPath.cleanPath();
        }
        this.compress = getSetting(UfileClientSettings.COMPRESS, metadata);
        this.chunkSize = getSetting(UfileClientSettings.CHUNK_SIZE, metadata);
        logger.trace("using bucket [{}], base_path [{}], chunk_size [{}], compress [{}]", bucket,
            basePath, chunkSize, compress);
        blobStore = new UfileBlobStore(env.settings(), bucket, ufileService);
    }

    @Override protected BlobStore blobStore() {
        return this.blobStore;
    }

    @Override protected BlobPath basePath() {
        return this.basePath;
    }

    @Override protected boolean isCompress() {
        return compress;
    }


    @Override protected ByteSizeValue chunkSize() {
        return chunkSize;
    }


    public static <T> T getSetting(Setting<T> setting, RepositoryMetaData metadata) {
        T value = setting.get(metadata.settings());
        if (value == null) {
            throw new RepositoryException(metadata.name(),
                "Setting [" + setting.getKey() + "] is not defined for repository");
        }
        if ((value instanceof String) && (Strings.hasText((String) value)) == false) {
            throw new RepositoryException(metadata.name(),
                "Setting [" + setting.getKey() + "] is empty for repository");
        }
        return value;
    }
}
