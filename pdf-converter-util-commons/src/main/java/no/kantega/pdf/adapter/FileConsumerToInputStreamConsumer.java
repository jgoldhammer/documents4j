package no.kantega.pdf.adapter;

import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;
import no.kantega.pdf.api.IFileConsumer;
import no.kantega.pdf.api.IInputStreamConsumer;
import no.kantega.pdf.throwables.FileSystemReadWriteException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

class FileConsumerToInputStreamConsumer implements IInputStreamConsumer {

    private final File file;
    private final IFileConsumer fileConsumer;

    public FileConsumerToInputStreamConsumer(File file, IFileConsumer fileConsumer) {
        this.file = file;
        this.fileConsumer = fileConsumer;
    }

    @Override
    public void onComplete(InputStream inputStream) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.getChannel().lock();
            try {
                ByteStreams.copy(inputStream, fileOutputStream);
            } finally {
                Closeables.close(inputStream, true);
                // Note: This will implicitly release the file lock.
                Closeables.close(fileOutputStream, false);
            }
        } catch (IOException e) {
            throw new FileSystemReadWriteException(String.format("Could not copy result to %s", file), e);
        }
        fileConsumer.onComplete(file);
    }

    @Override
    public void onCancel() {
        fileConsumer.onCancel(file);
    }

    @Override
    public void onException(Exception e) {
        fileConsumer.onException(file, e);
    }
}
