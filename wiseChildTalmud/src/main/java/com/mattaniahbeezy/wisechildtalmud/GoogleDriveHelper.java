package com.mattaniahbeezy.wisechildtalmud;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Mattaniah on 6/18/2015.
 */
public class GoogleDriveHelper implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "GoogleDriveHelper ";

    GoogleApiClient mGoogleApiClient;
    Activity context;

    public static final String notesFolderTitle = "Talmud Notes";

    boolean noteCreatedInDrive = false;

    public GoogleDriveHelper(Activity context) {
        this.context = context;
    }

    public void connect() {
        if (mGoogleApiClient == null)
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        if (!mGoogleApiClient.isConnected() && !mGoogleApiClient.isConnecting())
            mGoogleApiClient.connect();
    }

    public void disconnect() {
        if (mGoogleApiClient == null)
            return;
        if (mGoogleApiClient.isConnected())
            mGoogleApiClient.disconnect();
    }

    private void syncAllNotes() {
        List<NotesUtil> allNotes = NotesUtil.getAllNotes(context);
        for (NotesUtil util : allNotes) {
            int workingTractateIndex = util.tractateIndex;
            DriveFolder tractateFolder = getDriveFolder(Tractate.masechtosBavli[workingTractateIndex]);
            Iterator<String> iterator = util.getIterator();
            List<String> deleteKeys = util.getKeysToDelete();
            while (iterator.hasNext()) {
                String fileName = iterator.next();
                NotesUtil.Note note = util.getNote(fileName);
                DriveFile driveFile = getDriveFile(fileName + ".txt", tractateFolder, note);
                Metadata fileMetadata = driveFile.getMetadata(mGoogleApiClient).await().getMetadata();
                if (fileMetadata.isTrashed())
                    driveFile.untrash(mGoogleApiClient).await();

                if (fileMetadata.getModifiedDate().after(new Date(note.lastUpdated)) && !noteCreatedInDrive) {
                    note.setNoteText(getFileContents(driveFile));
                    Log.d(TAG, note.getNoteText() + " Downloading From Drive");
                } else
                    writeContentsToFile(note.getNoteText(), driveFile);

                if (deleteKeys.contains(fileName)) {
                    driveFile.delete(mGoogleApiClient);
                    deleteKeys.remove(fileName);
                }
                util.updateNote(fileName, note);

                noteCreatedInDrive = false;
            }
            util.setKeysToDelete(deleteKeys);
            util.saveAllNotes();
        }
        Log.d(TAG, Drive.DriveApi.requestSync(mGoogleApiClient).await().getStatusMessage());
    }


    private boolean writeContentsToFile(String contents, DriveFile file) {
        DriveApi.DriveContentsResult driveContentsResult = file.open(
                mGoogleApiClient, DriveFile.MODE_WRITE_ONLY, null).await();
        if (!driveContentsResult.getStatus().isSuccess()) {
            return false;
        }
        DriveContents driveContents = driveContentsResult.getDriveContents();
        return overwriteDriveContents(driveContents, contents);
    }

    private boolean overwriteDriveContents(DriveContents driveContents, String note) {
        try {
            OutputStream outputStream = driveContents.getOutputStream();
            outputStream.write(note.getBytes());
            com.google.android.gms.common.api.Status status =
                    driveContents.commit(mGoogleApiClient, null).await();
            outputStream.close();
            return status.getStatus().isSuccess();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String getFileContents(DriveFile file) {
        DriveContents contents = file.open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null).await().getDriveContents();
        InputStream inputStream = contents.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder builder = new StringBuilder();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            inputStream.close();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        contents.discard(mGoogleApiClient);
        return builder.toString();
    }


    private DriveFile getDriveFile(String fileName, DriveFolder folder, NotesUtil.Note note) {
        DriveId fileId = getFolderId(fileName, folder);
        if (fileId != null) {
            DriveFile file = Drive.DriveApi.getFile(mGoogleApiClient, fileId);
            return file;
        }
        noteCreatedInDrive = true;
        return Drive.DriveApi.getFile(mGoogleApiClient, createFile(fileName, folder, note));
    }

    private DriveId createFile(String fileName, DriveFolder folder, NotesUtil.Note note) {
        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle(fileName)
                .setMimeType("text/plain")
                .build();
        DriveContents driveContents = Drive.DriveApi.newDriveContents(mGoogleApiClient).await().getDriveContents();
        return folder.createFile(mGoogleApiClient, changeSet, driveContents).await().getDriveFile().getDriveId();
    }


    private DriveFolder getDriveFolder(String folderName) {
        DriveId folderId = getFolderId(folderName, getNotesRootFolder());
        if (folderId != null) {
            DriveFolder folder = Drive.DriveApi.getFolder(mGoogleApiClient, folderId);
            if (folder.getMetadata(mGoogleApiClient).await().getMetadata().isTrashed()) {
                folder.untrash(mGoogleApiClient).await();
            }
            return folder;
        }
        return createFolder(folderName, getNotesRootFolder());
    }

    private DriveFolder getNotesRootFolder() {
        DriveId rootId = getFolderId(notesFolderTitle);
        if (rootId != null) {
            DriveFolder folder = Drive.DriveApi.getFolder(mGoogleApiClient, rootId);
            if (folder.getMetadata(mGoogleApiClient).await().getMetadata().isTrashed())
                folder.untrash(mGoogleApiClient).await();
            return folder;
        }
        return createFolder(notesFolderTitle, null);
    }

    /*Method checks for Drive ID.
    @returns null if there is no DriveID, or if item is trashed.
    @returns Drive ID of first non-trashed name match in supplied folder*/
    private DriveId getFolderId(String name, DriveFolder folder) {
        if (folder == null)
            folder = Drive.DriveApi.getRootFolder(mGoogleApiClient);
        Query query = new Query.Builder()
                .addFilter(Filters.eq(SearchableField.TITLE, name))
                .build();
        try {
            MetadataBuffer buffer = folder.queryChildren(mGoogleApiClient, query).await().getMetadataBuffer();
            for (Metadata metadata : buffer) {
                if (!metadata.isTrashed()) {
                    DriveId id = metadata.getDriveId();
                    buffer.close();
                    return id;
                }
            }
            buffer.close();
        } catch (IndexOutOfBoundsException e) {
        } catch (IllegalStateException e) {
        }
        return null;
    }

    private DriveId getFolderId(String name) {
        return getFolderId(name, null);
    }

    public DriveFolder createFolder(String name, DriveFolder root) {
        if (root == null)
            root = Drive.DriveApi.getRootFolder(mGoogleApiClient);
        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle(name)
                .build();
        root.createFolder(mGoogleApiClient, changeSet).await();
        return Drive.DriveApi.getFolder(mGoogleApiClient, getFolderId(name, root));
    }


    @Override
    public void onConnected(Bundle bundle) {
        new SycnNotes().execute();
    }

    private class SycnNotes extends AsyncTask {
        @Override
        protected Object doInBackground(Object[] params) {
            try {
                syncAllNotes();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            disconnect();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }


    @Override
    public void onConnectionFailed(ConnectionResult result) {

    }
}
