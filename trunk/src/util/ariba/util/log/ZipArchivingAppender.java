/*
    Copyright 1996-2008 Ariba, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

    $Id: //ariba/platform/util/core/ariba/util/log/ZipArchivingAppender.java#4 $
*/


package ariba.util.log;

import java.io.File;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry; 
import java.util.zip.ZipOutputStream;
import org.apache.log4j.helpers.LogLog;

/**
   This class extends ariba.util.log.ArhivingAppender to provide
   zipping support.
   @aribaapi documented
*/


public class ZipArchivingAppender extends ArchivingAppender {
    
    public ZipArchivingAppender () 
    {
        super();
    }
    
    protected LogFile createLogFile(String fileName)
    {
        LogFile logFile = super.createLogFile(fileName);
        if (logFile.archivedFileName() != null ) {
          zip(archiveDirectory, logFile.archivedFileName());
        }
        return logFile;
    }
    
    
    /**
       Zip the already archived log file

       @aribaapi private
    */
    
    public void zip(File archiveDirectory , String archiveFileName)
    {
        ZipOutputStream out = null;
        BufferedInputStream in = null;

        File saveToFile =
            new File(archiveDirectory, archiveFileName);
        try {
            out = new ZipOutputStream(new BufferedOutputStream(
                    new FileOutputStream(saveToFile.getAbsolutePath() + ".gz")));
            in = new BufferedInputStream(new FileInputStream(saveToFile.getAbsolutePath()));
            int count;
            byte[] data = new byte[1000];
            out.putNextEntry(new ZipEntry(saveToFile.getAbsolutePath()));
            while ((count = in.read(data, 0, 1000)) != -1) {
                out.write(data, 0, count);
            }
            
        } catch (IOException iex) {
            LogLog.error("Error zipping log file " +
                    archiveFileName + ": " + iex.getMessage());
        } catch (Exception ex) {
            LogLog.error("Error zipping log file " +
                    archiveFileName + ": " + ex.getMessage());
        } finally {
          try {
            if(in != null ) {
              in.close();
            }
            if(out != null ) {
              out.flush();
              out.close();
            }
          }
          catch (IOException iex) {
            LogLog.error("Error zipping log file " +
                    archiveFileName + ": " + iex.getMessage());
          }
        }
        saveToFile.delete();
        
                    
    }

}

