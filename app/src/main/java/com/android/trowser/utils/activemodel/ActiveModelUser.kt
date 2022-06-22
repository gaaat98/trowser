package com.android.trowser.utils.activemodel

/*
* If you implement this interface then make sure that you manually marked as needless
* (@see com.android.trowser.utils.statemodel.ActiveModelsRepository#markAsNeedless())
* all your "active models" when they are not needed (on your component onDestroy() or similar)
 */
interface ActiveModelUser