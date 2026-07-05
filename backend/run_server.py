import os
import sys

import uvicorn

if __name__ == "__main__":
    # When frozen by PyInstaller, run relative to the exe's own directory
    # so customs_sync.db lives next to it instead of a temp extraction dir.
    if getattr(sys, "frozen", False):
        os.chdir(os.path.dirname(sys.executable))

    from main import app

    uvicorn.run(app, host="0.0.0.0", port=8000)
