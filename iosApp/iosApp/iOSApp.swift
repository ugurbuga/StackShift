import FirebaseCore
import SwiftUI

private enum StackShiftFirebaseConfig {
    static func loadOptions() -> FirebaseOptions? {
        guard let url = Bundle.main.url(forResource: "google", withExtension: "properties"),
              let content = try? String(contentsOf: url, encoding: .utf8) else {
            return nil
        }

        let values = parse(content)
        guard let appId = nonEmpty(values["firebase.ios.appId"]),
              let senderId = nonEmpty(values["firebase.gcmSenderId"] ?? values["firebase.projectNumber"]),
              let apiKey = nonEmpty(values["firebase.ios.apiKey"]),
              let projectId = nonEmpty(values["firebase.projectId"]) else {
            return nil
        }

        let options = FirebaseOptions(googleAppID: appId, gcmSenderID: senderId)
        options.apiKey = apiKey
        options.projectID = projectId
        if let bundleId = nonEmpty(values["firebase.ios.bundleId"]) ?? Bundle.main.bundleIdentifier {
            options.bundleID = bundleId
        }
        options.storageBucket = nonEmpty(values["firebase.storageBucket"])
        options.clientID = nonEmpty(values["firebase.ios.clientId"])
        return options
    }

    private static func parse(_ content: String) -> [String: String] {
        var values: [String: String] = [:]

        for line in content.components(separatedBy: .newlines) {
            let trimmedLine = line.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !trimmedLine.isEmpty,
                  !trimmedLine.hasPrefix("#"),
                  let separatorIndex = trimmedLine.firstIndex(of: "=") else {
                continue
            }

            let key = String(trimmedLine[..<separatorIndex]).trimmingCharacters(in: .whitespaces)
            let value = String(trimmedLine[trimmedLine.index(after: separatorIndex)...]).trimmingCharacters(in: .whitespaces)
            values[key] = value
        }

        return values
    }

    private static func nonEmpty(_ value: String?) -> String? {
        value?.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty
    }
}

private extension String {
    var nilIfEmpty: String? {
        isEmpty ? nil : self
    }
}

@main
struct iOSApp: App {
    init() {
        if FirebaseApp.app() == nil {
            if let options = StackShiftFirebaseConfig.loadOptions() {
                FirebaseApp.configure(options: options)
            } else if Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil {
                FirebaseApp.configure()
            }
        }
        StackShiftAdBridge.shared.start()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}