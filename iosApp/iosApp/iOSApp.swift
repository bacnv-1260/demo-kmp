import SwiftUI
import shared

@main
struct iOSApp: App {
    init() {
        let userDao = UserDao()
        let dataSource = UserDaoLocalDataSource(userDao: userDao)
        KoinIosHelperKt.doInitKoinIos(userLocalDataSource: dataSource)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
