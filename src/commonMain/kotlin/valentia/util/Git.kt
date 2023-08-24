package valentia.util

import valentia.ExecResult
import valentia.ExternalInterface

object Git {
    fun git(vararg params: String, cwd: LocalFile = LocalFile(".")): ExecResult {
        return ExternalInterface.exec("git", *params, cwd = cwd.fullPath)
    }

    fun clone(repoFolder: LocalFile, repo: String) {
        git("clone", repo, repoFolder.fullPath)
    }

    fun pull(repoFolder: LocalFile) {
        git("pull", cwd = repoFolder)
    }

    fun checkout(repoFolder: LocalFile, ref: String) {
        git("checkout", ref, cwd = repoFolder)
    }

    fun archive(repoFolder: LocalFile, targetFile: LocalFile, path: String = "", commit: String = "HEAD", format: String = "tar") {
        //git archive --format=tar HEAD~125:src
        git("archive", "-o", targetFile.fullPath, "--format=$format", "$commit:$path", cwd = repoFolder)
    }
}