//
//  NetworkGuidView.swift
//  DwebBrowser
//
//  Created by ui06 on 7/24/23.
//

import SwiftUI

struct NetworkGuidView: View {
    private var imageWidth: CGFloat { screen_width - 30 }
    private var imageHeight: CGFloat { imageWidth * CGFloat(85.0 / 117) }
    var body: some View {
        ZStack(alignment: .top){
            Color.bkColor
            VStack {
                Group{
                    Spacer().frame(height: 10)

                    RoundedRectangle(cornerRadius: 2.5)
                        .frame(width: 60, height: 6)
                        .foregroundColor(.white)
                    Spacer().frame(height: 52)

                    Image("wifi_error")
                        .resizable()
                        .frame(width: 80, height: 80)
                    
                    Spacer().frame(height: 32)
                    
                    Text("检测到网络异常，可能存在以下问题")
                        .font(.system(size: 16).bold())
                    
                    Spacer().frame(height: 32)
                    
                    Text("1.网络连接未成功，请开启/检查/切换网络")
                        .font(.custom("Source Han Sans CN-Medium", size: 15))
                        .frame(width: screen_width - 30)
                    
                        .padding(.vertical, 15)
                        .background(.white)
                        .cornerRadius(8)
                    
                        .foregroundColor(.networkTipColor)
                }
                Spacer().frame(height: 16)
                Text("2.网络权限可能未开启，您可以在“设置”中检查无线数据设置”中检查无线数据")
                    .font(.custom("Source Han Sans CN-Medium", size: 15))
                    .foregroundColor(.networkTipColor)
                    .padding(15)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color.white)
                    .cornerRadius(8)

                    .frame(width: UIScreen.main.bounds.width - 30)
                    .padding(.horizontal, 15)
                    .lineSpacing(5)

                    .multilineTextAlignment(.center)

                Spacer().frame(height: 32)

                Text("如何开启网络权限？")
                    .font(.system(size: 12))
                    .foregroundColor(.networkGuidColor)

                TabView {
                    Image("network_1")
                        .resizable()
                        .frame(width: imageWidth, height: imageHeight)
                    Image("network_2")
                        .resizable()
                        .frame(width: imageWidth, height: imageHeight)
                    Image("network_3")
                        .resizable()
                        .frame(width: imageWidth, height: imageHeight)
                }
                .frame(height: imageHeight + 80)
                .padding(.top, -35)
                .tabViewStyle(.page(indexDisplayMode: .always))
                .indexViewStyle(.page(backgroundDisplayMode: .always))
            }
        }
    }
}

struct NetworkGuidView_Previews: PreviewProvider {
    static var previews: some View {
        NetworkGuidView()
    }
}
