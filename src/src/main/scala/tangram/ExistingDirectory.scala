package tangram

import java.io.File

///////////////////////////////////////////////////////////

/**
 * Holds directories that exist and are actual directories (not files).
 */
case class ExistingDirectory(directory: File) {
  require(directory.isFile, s"${directory} is not an existing directory.")
}

object ExistingDirectory {
  def apply(filename: String): ExistingDirectory =
    ExistingDirectory(new File(filename))

  implicit def existingDirectory2File(self: ExistingDirectory) =
    self.directory
}